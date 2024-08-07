package net.essentuan.esl.rx.publishers

import net.essentuan.esl.rx.ISubscription
import net.essentuan.esl.rx.Producer
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

class MergedPublisher<T>(vararg val publishers: Publisher<T>) : Publisher<T> {
    override fun subscribe(s: Subscriber<in T>) {
        Subscription(s)
    }

    inner class Subscription(
        downstream: Subscriber<in T>
    ) : Producer<T>(downstream) {
        var size: Int = 0
        val upstream: Array<ISubscription?> = arrayOfNulls(publishers.size)

        override fun produce() {
            for (subscription in upstream)
                subscription?.request(requested)
        }

        init {
            publishers.forEachIndexed { i, it ->
                it.subscribe(object : Subscriber<T> {
                    override fun onSubscribe(s: ISubscription) {
                        synchronized(this@Subscription) {
                            upstream[i] = s
                            size++

                            if (size == upstream.size)
                                subscribe()
                        }
                    }

                    override fun onError(t: Throwable) = this@Subscription.error(t)

                    override fun onComplete() {
                        synchronized(this@Subscription) {
                            upstream[i] = null
                            size--

                            if (size == 0) this@Subscription.complete()
                        }
                    }

                    override fun onNext(t: T) = t.yield()
                })
            }
        }

        override fun cancel() = submit {
            super.cancel()

            for (i in upstream.indices) {
                upstream[i]?.cancel()
                upstream[i] = null
            }
        }
    }
}