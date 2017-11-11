package com.github.gumtreediff.client.kagami

import com.github.gumtreediff.actions.ActionGenerator
import com.github.gumtreediff.client.Option
import com.github.gumtreediff.client.Register
import com.github.gumtreediff.client.diff.AbstractDiffClient
import com.github.gumtreediff.gen.Registry
import com.github.gumtreediff.io.ActionsIoUtils
import com.github.gumtreediff.io.TreeIoUtils
import org.apache.commons.io.IOUtils
import spark.Spark.get
import spark.Spark.staticFileLocation
import java.io.FileReader

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
                        }
                )
    }

    override fun run() {
        configureSpark()
    }

    override fun newOptions() = Options()

    private fun configureSpark() {
        staticFileLocation("/dist")
        get("/api/:type/source") { req, res ->
            res.header("Access-Control-Allow-Origin", "*")
            val type = req.params(":type")
            val file = when (type) {
                "src" -> opts.src
                "dest" -> opts.dst
                else -> ""
            }
            val body = IOUtils.toString(FileReader(file))
            return@get body
        }
        get("/api/:type/tree") { req, res ->
            res.header("Access-Control-Allow-Origin", "*")
            val type = req.params(":type")
            val body = when (type) {
                "src" -> TreeIoUtils.toJson(srcTreeContext).toString()
                "dest" -> TreeIoUtils.toJson(dstTreeContext).toString()
                else -> ""
            }
            res.type("application/json")
            return@get body
        }
        get("/api/match") { req, res ->
            res.header("Access-Control-Allow-Origin", "*")
            val m = matchTrees()
            val g = ActionGenerator(srcTreeContext.root, dstTreeContext.root, m.mappings)
            g.generate()
            val actions = g.actions
            var body = ""
            try {
                body = ActionsIoUtils.toJson(srcTreeContext, actions, m.mappings).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            res.type("application/json")
            return@get body
        }
    }
}
