package net.essentuan.esl.iteration.extensions

inline infix fun <T> Sequence<T>.iterate(executor: Iterator<T>.(T) -> Unit) =
    iterator() iterate executor

inline fun <reified T> Sequence<T>.toTypedArray() =
    toList().toTypedArray()