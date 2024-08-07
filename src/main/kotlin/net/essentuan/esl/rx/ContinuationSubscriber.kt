package net.essentuan.esl.rx

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

abstract class ContinuationSubscriber<IN, OUT>(
    private val cont: Continuation<OUT>,
    private val n: Long = Int.MAX_VALUE.toLong()
) : Subscriber<IN> {
    private var subscription: Subscription? = null
    var open: Boolean = false
        private set

    final override fun onSubscribe(s: Subscription) {
        subscription = s
        open = true
        s.request(n)
    }

    @Synchronized
    fun yield(value: OUT) {
        open = false
        subscription?.cancel()
        cont.resume(value)
    }

    @Synchronized
    fun error(ex: Throwable) {
        open = false
        subscription?.cancel()
        cont.resumeWithException(ex)
    }

    override fun onError(ex: Throwable) = error(ex)
}