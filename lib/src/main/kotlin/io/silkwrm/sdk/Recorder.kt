package io.silkwrm.sdk

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.io.File

data class ImageUploadResponse(var imageId: String = "",
                               var uploadUrl:String = "")

data class Result<T>(var result: Boolean = false, var response: T? = null)

class Recorder {
    fun getAllImages(dir: String): List<File> {
        var dirFile = File(dir)
        return dirFile.listFiles().toList()
                .filter { x -> x.absolutePath.endsWith(".png") }
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
            System.out.println("Uploading file: " + file)
            val result : Result<ImageUploadResponse>  =
                Fuel.post("https://silkwrm.tdrhq.com/api/prepare-upload", listOf("name" to "foo", "hash" to "blah"))
                  .responseObject<Result<ImageUploadResponse>>(jacksonDeserializerOf(mapper)).third.get()

            System.out.println("Got upload url: " + result.response?.uploadUrl)
        }


    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Recorder().run(args)
        }


    }
}
