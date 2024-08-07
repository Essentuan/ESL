package net.essentuan.esl.rx

import org.reactivestreams.Publisher

interface PublisherScope<T> {
    val requested: Long

    suspend fun yield(value: T)

    suspend fun yieldAll(value: Publisher<T>)
}

suspend inline fun <T> PublisherScope<T>.yieldAll(iterator: Iterator<T>) {
    for (e in iterator)
        yield(e)
}

suspend inline fun <T> PublisherScope<T>.yieldAll(array: Array<T>) {
    if (array.isNotEmpty())
        return yieldAll(array.iterator())
}

suspend inline fun <T> PublisherScope<T>.yieldAll(elements: Iterable<T>) {
    if (elements !is Collection || elements.isNotEmpty())
        yieldAll(elements.iterator())
}

suspend inline fun <T> PublisherScope<T>.yieldAll(sequence: Sequence<T>) {
    yieldAll(sequence.iterator())
}