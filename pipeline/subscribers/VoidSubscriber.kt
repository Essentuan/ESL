package net.essentuan.esl.async.pipeline.subscribers

import net.essentuan.esl.async.pipeline.Subscriber
import org.reactivestreams.Publisher

class VoidSubscriber(publisher: Publisher<Unit>) : Subscriber<Unit, Unit>() {
    init {
        publisher.subscribe(this)
    }

    override fun result() = Unit

    override fun onNext(unused: Unit) {
    }
}
