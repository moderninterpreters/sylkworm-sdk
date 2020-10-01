package io.silkwrm.sdk

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

data class ImageUploadResponse(var imageId: String? = "",
                               var uploadUrl:String? = "")

data class Result<T>(var result: Boolean = false, var response: T? = null)

class Recorder {
    fun getAllImages(dir: String) = run {
        var dirFile = File(dir)
        dirFile.listFiles().toList()
                .filter { x -> x.absolutePath.endsWith(".png") }
    }

    fun getDigest(file: File) = run {
        Files.asByteSource(file).hash(Hashing.sha256()).toString()
    }

    private fun run(args: Array<String>) {
        var options = Options()
        options.addOption("d", "dir", true, "Directory with screenshots")
        var parser = DefaultParser()
        var cli = parser.parse(options, args)

        var dir = cli.getOptionValue('d')

        val mapper = ObjectMapper()
        var allImages = getAllImages(dir);
        for (file in allImages) {
            uploadImage(file, mapper)
        }


    }

    private fun uploadImage(file: File, mapper: ObjectMapper) = run {
        System.out.println("Uploading file: " + file)
        val hash = getDigest(file)
        val result: Result<ImageUploadResponse> =
            Fuel.post("https://silkwrm.tdrhq.com/api/prepare-upload", listOf("name" to file.name, "hash" to hash))
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
