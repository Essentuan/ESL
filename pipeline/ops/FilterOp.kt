package net.essentuan.esl.async.pipeline.ops

import net.essentuan.esl.async.pipeline.Gatherer
import java.util.function.Predicate

class FilterOp<T>(private val predicate: Predicate<in T>) : Gatherer<T, T>() {
    override fun onNext(value: T) {
        if (predicate.test(value))
            accept(value)
    }
}