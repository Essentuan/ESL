package net.essentuan.esl.rx

import net.essentuan.esl.iteration.BreakException

interface RxIterator<T> {
    var batchSize: Long

    suspend operator fun hasNext(): Boolean

    operator fun next(): T
}

suspend inline infix fun <T, ITER: RxIterator<T>> ITER.iterate(
    block: ITER.(T) -> Unit
) {
    try {
        while (hasNext())
            block(next())
    } catch (ex: Throwable) {
        if (ex is BreakException)
            return
        else
            throw ex
    }
}