package net.essentuan.esl.iteration.extensions

import java.util.stream.Stream

inline infix fun <T> Stream<T>.iterate(executor: Iterator<T>.(T) -> Unit) =
    iterator() iterate executor

fun <T> Stream<T>.iterable(): Iterable<T> = Iterable { this.iterator() }

@Suppress("UNCHECKED_CAST")
fun <T: Any> Stream<T?>.nonNull(): Stream<T> = filter { it != null } as Stream<T>

//@Suppress("UNCHECKED_CAST")
//fun <T: Any> Pipeline<T?>.nonNull(): Pipeline<T> = net.essentuan.esl.filter { it != null} as Pipeline<T>