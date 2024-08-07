package net.essentuan.esl.async.pipeline

import net.essentuan.esl.future.AbstractFuture
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

typealias RxSubscriber<T> = Subscriber<T>

abstract class Subscriber<In, Out> : AbstractFuture<Out>(), RxSubscriber<In> {
    override fun onSubscribe(s: Subscription) {
        s.request(Int.MAX_VALUE.toLong())
    }

    override fun onError(t: Throwable) {
        toss(t)
    }

    protected abstract fun result(): Out

    override fun onComplete() {
        complete(result())
    }
}
