package net.essentuan.esl.iteration.extensions

import net.essentuan.esl.iteration.Iterators
import net.essentuan.esl.iteration.iterators.MappedIter
import java.util.function.Function

fun <T> MutableIterator<T>.concat(vararg iters: MutableIterator<T>): MutableIterator<T> {
    val list = mutableListOf(this)
    list.addAll(iters)

    return Iterators.join(*list.toTypedArray())
}

fun <T, U> MutableIterator<T>.concat(vararg iters: MutableIterator<U>, mapper: Function<U, T>): MutableIterator<T> {
    return Iterators.join(
        this,
        Iterators.join(*iters).map(mapper)
    )
}

fun <T, U> MutableIterator<T>.flatMap(mapper: Function<T, MutableIterator<U>>): MutableIterator<U> {
    return Iterators.concat(map(mapper))
}

fun <T, U> MutableIterator<T>.map(mapper: Function<T, U>): MutableIterator<U> {
    return MappedIter.Mutable(this, mapper)
}

fun <T, U> MutableIterator<T>.cast(cls: Class<U>): MutableIterator<U> {
    return map(cls::cast)
}

@Suppress("UNCHECKED_CAST")
fun <T, U> MutableIterator<T>.cast(): MutableIterator<U> {
    return map { o -> o as U }
}