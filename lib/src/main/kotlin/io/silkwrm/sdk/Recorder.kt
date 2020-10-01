package io.silkwrm.sdk

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.io.File

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


        var allImages = getAllImages(dir);
        for (file in allImages) {
            System.out.println("got file: " + file)
        }
        System.out.println("hello " + dir)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Recorder().run(args)
        }


    }
}
