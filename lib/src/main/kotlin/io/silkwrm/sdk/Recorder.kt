package io.silkwrm.sdk

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.io.File
import java.lang.RuntimeException

data class ImageUploadResponse(var imageId: String? = "",
                               var uploadUrl:String? = "")

data class CreateRunResponse(var runId: Int = 0)

data class Result<T>(var result: Boolean = false, var response: T? = null)
data class ScreenshotRecord(val name: String, val imageId: String)

class Recorder {
    var mapper = ObjectMapper()

    fun getAllImages(dir: String) = run {
        var dirFile = File(dir)
        dirFile.listFiles().toList()
                .filter { x -> x.absolutePath.endsWith(".png") }
    }

    fun getDigest(file: File) = run {
        Files.asByteSource(file).hash(Hashing.sha256()).toString()
    }

    private fun run(args: Array<String>) {
        val options = Options()
        options.addOption("d", "dir", true, "Directory with screenshots")
        options.addOption("c", "channel", true, "Channel name under which the screenshots should go under")

        val parser = DefaultParser()
        val cli = parser.parse(options, args)

        val dir = cli.getOptionValue('d')
        val channel = cli.getOptionValue('c')

        if (channel.isNullOrEmpty())
            throw RuntimeException("empty channel")

        if (dir.isNullOrEmpty())
            throw RuntimeException("no directory specified")

        val allImages = getAllImages(dir).map {file ->
            val response = uploadImage(file)
            ScreenshotRecord(file.name, response.imageId!!)
        }

        makeRun(channel, allImages)

    }

    private fun makeRun(channel: String, allImages: List<ScreenshotRecord>) = run {
        val recordsJson = mapper.writeValueAsString(allImages)
        Fuel.post(buildUrl("/api/run"), listOf("channel" to channel, "screenshot-records" to recordsJson))
            .responseObject<Result<CreateRunResponse>>(jacksonDeserializerOf(mapper))
    }

    fun buildUrl(url: String) = run {
        "https://sylkworm.io" + url
    }

    private fun uploadImage(file: File) = run {
        System.out.println("Uploading file: " + file)
        val hash = getDigest(file)
        val result: Result<ImageUploadResponse> =
            Fuel.post(buildUrl("/api/prepare-upload"), listOf("name" to file.name, "hash" to hash))
                .responseObject<Result<ImageUploadResponse>>(jacksonDeserializerOf(mapper)).third.get()

        val response = result.response!!

        if (response.imageId.isNullOrEmpty()) {
            // let's start the upload process
            System.out.println("New image, uploading...")
            Fuel.upload(response.uploadUrl!!)
                .add(FileDataPart(file, name="files", filename=file.name))
                .responseObject<Result<ImageUploadResponse>>(jacksonDeserializerOf(mapper)).third.get().response!!
        } else {
            System.out.println("reusing existing image")
            response
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Recorder().run(args)
        }


    }
}
