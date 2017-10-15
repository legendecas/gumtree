/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.gen.antlr4.python3

import com.github.gumtreediff.gen.Register
import com.github.gumtreediff.gen.TreeGenerator
import com.github.gumtreediff.tree.ITree
import com.github.gumtreediff.tree.TreeContext
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.antlr.v4.runtime.tree.TerminalNodeImpl
import java.io.Reader
import java.util.*

@Suppress("unused")
@Register(id = "python3-antlr", accept = arrayOf("\\.py$"))
class Python3TreeGenerator : TreeGenerator() {

    override fun generate(r: Reader?): TreeContext {
        val inputStream = CharStreams.fromReader(r)
        val lexer = Python3Lexer(inputStream)
        val tokenStream = CommonTokenStream(lexer)
        val parser = Python3Parser(tokenStream)

        val vocabulary = parser.vocabulary
        val ruleNames = parser.ruleNames
        val ctx = TreeContext()
        val parseTree = parser.file_input()
        ParseTreeWalker.DEFAULT.walk(Listener(ctx, vocabulary, ruleNames, tokenStream), parseTree)

        return ctx
    }

    class Listener(private val context: TreeContext, private val vocabulary: Vocabulary, private val ruleNames: Array<String>, private val tokenStream: CommonTokenStream) : Python3BaseListener() {

        private val trees = ArrayDeque<ITree>()

        @Suppress("FunctionName")
        override fun exitFile_input(ctx: Python3Parser.File_inputContext?) {
            super.exitFile_input(ctx)
            ctx?.children?.forEach { child ->
                buildTree(context, child)
            }
        }

        private fun buildTree(context: TreeContext, t: ParseTree) {
            when (t) {
                is ParserRuleContext -> {
                    val ruleIndex = t.ruleIndex
                    val ruleName = ruleNames[ruleIndex]
                    val tree = context.createTree(ruleIndex, null, ruleName)

                    val startToken = t.start
                    val stopToken = t.stop
                    val start = startToken.startIndex
                    val stop = stopToken.stopIndex
                    tree.pos = start
                    tree.length = stop - start + 1

                    if (trees.isEmpty()) {
                        context.root = tree
                    } else {
                        tree.setParentAndUpdateChildren(trees.peek())
                    }

                    if (t.childCount == 1 && t.getChild(0) is TerminalNodeImpl) {
                        tree.label = t.getChild(0).text
                    } else if (t.childCount > 0) {
                        trees.push(tree)
                        t.childCount.times { idx ->
                            buildTree(context, t.getChild(idx))
                        }
                        trees.pop()
                    }
                }
            }
        }

    }
}
