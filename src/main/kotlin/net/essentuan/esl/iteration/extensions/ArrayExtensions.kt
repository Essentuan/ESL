package net.essentuan.esl.iteration.extensions

inline infix fun <T> Array<T>.iterate(block: Iterator<T>.(T) -> Unit) =
    iterator() iterate block