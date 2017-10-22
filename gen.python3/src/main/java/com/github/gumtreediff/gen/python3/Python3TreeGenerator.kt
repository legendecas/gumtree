package com.github.gumtreediff.gen.python3

import com.beust.klaxon.*
import com.github.gumtreediff.gen.Register
import com.github.gumtreediff.gen.TreeGenerator
import com.github.gumtreediff.tree.ITree
import com.github.gumtreediff.tree.TreeContext
import org.apache.commons.io.IOUtils
import java.io.Reader
import java.lang.Exception
import java.nio.charset.Charset
import java.util.*

@Suppress("unused")
@Register(id = "python3", accept = arrayOf("\\.py$"))
class Python3TreeGenerator : TreeGenerator() {
    override fun generate(r: Reader?): TreeContext {
        val script = "/Users/lucas/Developer/gumtree/gen.python3/src/main/resources/main.py"
        val program = Runtime.getRuntime().exec("python $script")
        val source = IOUtils.toString(r)
        val rStream = IOUtils.toInputStream(source, "UTF-8")
        IOUtils.copy(rStream, program.outputStream, 256)
        program.outputStream.close()

        return try {
            val p = IOUtils.toString(program.inputStream, Charset.forName("UTF-8"))
            val builder = StringBuilder(p)
            val parser = Parser()
            val json = parser.parse(builder) as JsonObject
            val ctx = TreeContext()
            traversal(json, ctx)
            ctx
        } catch (err: Exception) {
            err.printStackTrace()
            TreeContext()
        }
    }

    private val deque = ArrayDeque<ITree>()

    private fun traversal(json: JsonObject?, ctx: TreeContext) {
        if (json == null) {
            return
        }
        val label = json.string("label") ?: ITree.NO_LABEL
        val type = json.int("type") ?: return
        val typeLabel = json.string("typeLabel") ?: return

        val node = ctx.createTree(type, label, typeLabel)

        val start = json.int("start")
        val end = json.int("end")
        if (start != null && end != null) {
            node.pos = start
            node.length = end - start
        }

        if (deque.isEmpty()) {
            ctx.root = node
        } else {
            node.setParentAndUpdateChildren(deque.peek())
        }

        val children = json.array<JsonObject?>("children") ?: return
        deque.push(node)
        children.forEach { child ->
            traversal(child, ctx)
        }
        deque.pop()
    }
}
