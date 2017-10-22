package com.github.gumtreediff.gen.python3

import org.apache.commons.io.IOUtils
import java.nio.charset.Charset

fun main(args: Array<String>) {
    val script = "/Users/lucas/Developer/gumtree/gen.python3/src/main/resources/main.py"
    val fileContent = "def main():\n  pass\n"
    println("start execute")
    val program = Runtime.getRuntime().exec("python $script")
    val r = IOUtils.toInputStream(fileContent, "UTF-8")
    IOUtils.copy(r, program.outputStream, 256)
    program.outputStream.close()


    val p = IOUtils.toString(program.inputStream, Charset.forName("UTF-8"))
    val builder = StringBuilder(p)
    println(builder.toString())
}
