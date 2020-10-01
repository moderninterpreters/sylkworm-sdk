package io.silkwrm.sdk

import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Options

class Recorder {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            var options = Options()
            options.addOption("d",  "dir",true, "Directory with screenshots")
            var parser = DefaultParser()
            var cli = parser.parse(options, args)

            var dir = cli.getOptionValue('d')

            System.out.println("hello " + dir)
        }
    }
}
