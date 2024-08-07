package net.essentuan.esl.async.pipeline.subscribers

import net.essentuan.esl.async.pipeline.Subscriber
import java.util.function.Consumer

class ForEachSubscriber<T>(consumer: Consumer<T>) : Subscriber<T, Unit>() {
    private val consumer: Consumer<in T> = consumer

    override fun onNext(t: T) {
        consumer.accept(t)
    }

    override fun result() {
        return
    }
}
