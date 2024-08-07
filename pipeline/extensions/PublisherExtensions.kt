package net.essentuan.esl.async.pipeline.extensions

import kotlinx.coroutines.suspendCancellableCoroutine
import net.essentuan.esl.async.pipeline.Pipeline
import net.essentuan.esl.async.pipeline.RxSubscriber
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun <T> Publisher<T>.pipe(): Pipeline<T> = Pipeline.of(this)

suspend fun <T> Publisher<T>.result(): List<T> = pipe().toList()

suspend fun <T> Publisher<T>.await() = suspendCancellableCoroutine { cont ->
    subscribe(object : RxSubscriber<T> {
        override fun onSubscribe(subscription: Subscription) = subscription.request(Int.MAX_VALUE.toLong())

        override fun onError(exception: Throwable) = cont.resumeWithException(exception)

        override fun onComplete() = cont.resume(Unit)

        override fun onNext(value: T) = Unit
    })
}
