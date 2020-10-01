package io.silkwrm.sdk

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options
import java.io.File

class Recorder {

    fun getAllImages(dir: String) {
        var dirFile = File(dir)
        for (file in dirFile.listFiles()) {
            System.out.println("got file: " + file)
        }
    }
    private fun run(args: Array<String>) {
        var options = Options()
        options.addOption("d", "dir", true, "Directory with screenshots")
        var parser = DefaultParser()
        var cli = parser.parse(options, args)

        var dir = cli.getOptionValue('d')

        getAllImages(dir)
        System.out.println("hello " + dir)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Recorder().run(args)
        }


    }
}
