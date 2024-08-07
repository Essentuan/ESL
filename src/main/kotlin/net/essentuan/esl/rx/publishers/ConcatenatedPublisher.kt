package net.essentuan.esl.rx.publishers

import net.essentuan.esl.rx.ISubscription
import net.essentuan.esl.rx.Producer
import net.essentuan.esl.rx.READY
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import java.util.LinkedList
import java.util.Queue

class ConcatenatedPublisher<T>(vararg val publishers: Publisher<T>) : Publisher<T> {
    override fun subscribe(s: Subscriber<in T>) { Subscription(s) }

    inner class Subscription(
        downstream: Subscriber<in T>
    ) : Producer<T>(downstream), Subscriber<T> {
        val queue: Queue<Publisher<T>> = LinkedList(publishers.asList())
        var active: ISubscription? = null

        init { next() }

        fun next() = submit {
            when {
                queue.isEmpty() -> complete()
                else -> queue.poll().subscribe(this)
            }
        }

        override fun produce() = submit {
            if (active == null)
                next()

            active?.request(requested)
        }

        override fun cancel() = submit {
            super.cancel()

            active?.cancel()
            active = null

            queue.clear()
        }

        override fun onSubscribe(s: ISubscription) {
            active = s

            when {
                requested == READY -> subscribe()
                requested > 0 -> produce()
            }
        }

        override fun onError(t: Throwable) = error(t)

        override fun onComplete() = next()

        override fun onNext(t: T) = t.yield()
    }
}