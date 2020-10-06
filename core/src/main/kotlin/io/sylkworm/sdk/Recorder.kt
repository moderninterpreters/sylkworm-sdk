package io.sylkworm.sdk

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.jackson.jacksonDeserializerOf
import com.google.common.hash.Hashing
import com.google.common.io.Files
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.io.File
import java.lang.RuntimeException
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.util.Collections.emptyList
import javax.xml.stream.XMLInputFactory

data class ImageUploadResponse(var imageId: String? = "",
                               var uploadUrl:String? = "")

data class CreateRunResponse(var runId: Int = 0)

data class Result<T>(var result: Boolean = false, var response: T? = null, var error: String? = null)
data class ScreenshotRecord(val name: String, val imageId: String)

data class Credential(var apiKey: String = "",
                      var apiSecretKey: String = "")

fun readConfig() = run {
    val mapper = ObjectMapper()
    mapper.registerModule(KotlinModule())

    val homeStr = System.getProperty("user.home")
    if (homeStr.isNullOrEmpty()) {
        throw RuntimeException("user.home is not set")
    }
    val home = File(homeStr)
    val file = File(home, ".sylkworm")
    if (!file.exists()) {
        throw RuntimeException("Could not find config file at " + file)
    }
    val json = file.readText()
    //throw RuntimeException("got json: " + json)
    mapper.readValue<Credential>(json)
}

data class Screenshot(
        var description: String? = "",
        var name: String? = "",
        var test_class: String?="",
        var test_name: String? = "",
        var view_hierarchy: String? ="",
        var tile_width: Int? = null,
        var tile_height: Int? = null) {
}
class Screenshots() {

}



class Recorder() {
    var mapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getAllImages(dir: String) = run {
        var dirFile = File(dir)
        dirFile.listFiles().toList()
                .filter { x -> x.absolutePath.endsWith(".png") }
    }

    fun getDigest(file: File) = run {
        Files.asByteSource(file).hash(Hashing.md5()).toString()
    }

    private fun run(args: Array<String>) {
        mapper.registerModule(KotlinModule())
        val options = Options()
        options.addOption("d", "dir", true, "Directory with screenshots")
        options.addOption("c", "channel", true, "Channel name under which the screenshots should go under")

        val parser = DefaultParser()
        val cli = parser.parse(options, args)

        val dir = cli.getOptionValue('d')
        val channel = cli.getOptionValue('c')

        doRecorder(channel, dir)

    }

    fun readMetadata(file: File) = run {
        val mapper = XmlMapper()
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val f = XMLInputFactory.newFactory()
        val sr = f.createXMLStreamReader(file.inputStream())

        sr.next() // <screenshots>
        sr.next() // <screenshot>

        val screenshots = mutableListOf<Screenshot>()
        logger.debug("Current element has name: " + sr.name)

        while (sr.isStartElement) {

            logger.info("current name is: " + sr.name)

            if (sr.name.localPart.equals("screenshot")) {
                logger.info("Got screenshot tag")
                screenshots.add(mapper.readValue<Screenshot>(sr, Screenshot::class.java))
            }

            sr.next() // <screenshot> or </screenshots>
            logger.info("we're now at: " + sr.name)
        }

        screenshots.toList()
    }

    public fun doRecorder(channel: String, dir: String) {
        if (channel.isNullOrEmpty())
            throw RuntimeException("empty channel")

        if (dir.isNullOrEmpty())
            throw RuntimeException("no directory specified")

        val metadata = File(dir, "metadata.xml")
        val screenshots = readMetadata(metadata)

        logger.info("Got ${screenshots.size} screenshots to upload")
        val allImages = screenshots.map { screenshot ->
            // todo: get all the other tiles
            val file = File(dir, screenshot.name + ".png")
            val response = uploadImage(file)
            ScreenshotRecord(file.name, response.imageId!!)
        }

        makeRun(channel, allImages)
    }

    private fun makeRun(channel: String, allImages: List<ScreenshotRecord>) = run {
        logger.info("Finalizing the reporter run")
        val recordsJson = mapper.writeValueAsString(allImages)
        val resp = Fuel.post(buildUrl("/api/run"),
                  listOf("channel" to channel, "screenshot-records" to recordsJson,
                         "api-key" to readConfig().apiKey,
                         "api-secret-key" to readConfig().apiSecretKey))
            .responseObject<Result<CreateRunResponse>>(jacksonDeserializerOf(mapper)).second;

        if (resp.statusCode != 200) {
            throw RuntimeException("Failed to finalize run, got code ${resp.statusCode}, contact support@sylkworm.io for help")
        }
    }

    fun buildUrl(url: String) = run {
        "https://sylkworm.io" + url
    }

    private fun uploadImage(file: File) = run {
        logger.info("Uploading file: " + file)
        val hash = getDigest(file)
        val result: Result<ImageUploadResponse> =
            Fuel.post(buildUrl("/api/prepare-upload"),
                      listOf("name" to file.name, "hash" to hash,
                         "api-key" to readConfig().apiKey,
                         "api-secret-key" to readConfig().apiSecretKey))
                .responseObject<Result<ImageUploadResponse>>(jacksonDeserializerOf(mapper)).third.get()

        val response = result.response!!

        if (!response.uploadUrl.isNullOrEmpty()) {
            // let's start the upload process
            val arr = file.readBytes()
            logger.debug("New image, uploading to: " + response.uploadUrl)
            val code = Fuel.put(response.uploadUrl!!)
                        .body(file)
                        .response().second

            if (code.statusCode != 200) {
                throw RuntimeException("Error while uploading image data")
            }
        } else {
            logger.debug("reusing existing image")
        }
        response
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Recorder().run(args)
        }


    }
}
