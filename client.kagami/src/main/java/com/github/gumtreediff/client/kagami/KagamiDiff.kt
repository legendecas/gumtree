package com.github.gumtreediff.client.kagami

import com.github.gumtreediff.client.Option
import com.github.gumtreediff.client.Register
import com.github.gumtreediff.client.diff.AbstractDiffClient
import com.github.gumtreediff.gen.Registry
import com.github.gumtreediff.io.DirectoryComparator

import spark.Spark.*
import java.awt.print.Book



@Suppress("unused")
@Register(name = "Kagami", description = "An elegant web diff client", options = KagamiDiff.Options::class, priority = Registry.Priority.HIGH)
class KagamiDiff(args: Array<out String>?) : AbstractDiffClient<KagamiDiff.Options>(args) {
    class Options : AbstractDiffClient.Options() {
        private var defaultPort = Integer.parseInt(System.getProperty("gumtree.client.web.port", "4567"))
        var stdin = true

        override fun values(): Array<Option> =
            Option.Context.addValue(super.values(),
                    object : Option("--port", String.format("set server port (default to)", defaultPort), 1) {
                        override fun process(name: String, args: Array<String>) {
                            val p = Integer.parseInt(args[0])
                            if (p > 0)
                                defaultPort = p
                            else
                                System.err.printf("Invalid port number (%s), using %d\n", args[0], defaultPort)
                        }
                    },
                    object : Option("--no-stdin", String.format("Do not listen to stdin"), 0) {
                        override fun process(name: String, args: Array<String>) {
                            stdin = false
                        }
                    }
            )
    }

    override fun run() {


    }

    override fun newOptions() = Options()

    fun configureSpark() {
        staticFileLocation("/dist")
    }
}