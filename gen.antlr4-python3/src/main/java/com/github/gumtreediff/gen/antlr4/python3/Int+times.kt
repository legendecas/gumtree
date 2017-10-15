package com.github.gumtreediff.gen.antlr4.python3

fun Int.times(body: (Int) -> Unit) {
    return (0 until this).forEach(body)
}