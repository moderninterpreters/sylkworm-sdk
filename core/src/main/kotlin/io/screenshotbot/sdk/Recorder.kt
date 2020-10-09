package io.screenshotbot.sdk

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
import com.google.common.io.ByteSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.awt.image.Raster
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.util.Collections.emptyList
import javax.imageio.ImageIO
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
    val file = File(home, ".screenshotbot")
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
    var commit: String? = null
    private var githubRepo: String? = ""
    var mapper = ObjectMapper()
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getAllImages(dir: String) = run {
        var dirFile = File(dir)
        dirFile.listFiles().toList()
                .filter { x -> x.absolutePath.endsWith(".png") }
    }

    fun getDigest(data: ByteArray) = run {
        ByteSource.wrap(data).hash(Hashing.md5()).toString()
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

            if (sr.name.localPart.equals("screenshot")) {
                screenshots.add(mapper.readValue<Screenshot>(sr, Screenshot::class.java))
            }

            sr.next() // <screenshot> or </screenshots>
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
            val imgs = Array<Array<BufferedImage>>(screenshot.tile_height!!) { h->
                Array<BufferedImage>(screenshot.tile_width!!) { w->
                    val file = (if (h == 0 &&  w == 0) File(dir, screenshot.name + ".png")
                                else File(dir, "${screenshot.name}_${w}_${h}.png"))

                    ImageIO.read(file)
                }
            }
            val width = imgs[0].map{
                it.width
            }.sum()
            val height = imgs.map {
                it[0].height
            }.sum()

            val outputImg = BufferedImage(width, height, imgs[0][0].type)

            var h = 0;
            imgs.map {
                var w = 0;
                it.map {
                    outputImg.raster.setRect(w, h, it.raster)
                    w += it.width
                }
                h += it[0].height
            }

            val data = ByteArrayOutputStream()
            ImageIO.write(outputImg, "png", data)
            val response = uploadImage(screenshot.name + ".png", data.toByteArray())
            ScreenshotRecord(screenshot.name!!, response.imageId!!)
        }

        makeRun(channel, allImages)
    }

    private fun makeRun(channel: String, allImages: List<ScreenshotRecord>) = run {
        logger.info("Finalizing the reporter run")
        val recordsJson = mapper.writeValueAsString(allImages)
        val resp = Fuel.post(buildUrl("/api/run"),
                  listOf("channel" to channel, "screenshot-records" to recordsJson,
                         "github-repo" to githubRepo,
                         "commit" to commit,
                         "api-key" to readConfig().apiKey,
                         "api-secret-key" to readConfig().apiSecretKey))
            .responseObject<Result<CreateRunResponse>>(jacksonDeserializerOf(mapper)).second;

        if (resp.statusCode != 200) {
            throw RuntimeException("Failed to finalize run, got code ${resp.statusCode}, contact support@sylkworm.io for help")
        }
    }

    fun buildUrl(url: String) = run {
        "https://api.screenshotbot.io" + url
    }

    private fun uploadImage(fileName: String, data: ByteArray) = run {
        logger.info("Uploading file: " + fileName)
        val hash = getDigest(data)
        val result: Result<ImageUploadResponse> =
            Fuel.post(buildUrl("/api/prepare-upload"),
                      listOf("name" to fileName, "hash" to hash,
                         "api-key" to readConfig().apiKey,
                         "api-secret-key" to readConfig().apiSecretKey))
                .responseObject<Result<ImageUploadResponse>>(jacksonDeserializerOf(mapper)).third.get()

        val response = result.response!!

        if (!response.uploadUrl.isNullOrEmpty()) {
            // let's start the upload process

            logger.info("New image, never seen before. Uploading!")
            val code = Fuel.put(response.uploadUrl!!)
                        .body(data)
                        .response().second

            if (code.statusCode != 200) {
                throw RuntimeException("Error while uploading image data")
            }
        } else {
            logger.debug("reusing existing image")
        }
        response
    }

    fun setGithubRepo(githubRepo: String?) {
        this.githubRepo = githubRepo
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Recorder().run(args)
        }


    }
}
