package net.essentuan.esl.async.pipeline.ops

import net.essentuan.esl.async.pipeline.Gatherer
import java.util.function.Consumer

class PeekOp<T>(private val consumer: Consumer<in T>) : Gatherer<T, T>() {
    override fun onNext(value: T) {
        consumer.accept(value)
        super.onNext(value)
    }
}
