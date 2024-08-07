package net.essentuan.esl.rx

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.LinkedList
import java.util.Queue

const val READY: Long = Long.MIN_VALUE + 1
const val CLOSED: Long = Long.MIN_VALUE

typealias ISubscription = Subscription

abstract class Producer<T>(
    private val downstream: Subscriber<in T>
) : ISubscription {
    val buffer: Queue<T> = LinkedList()
    var requested: Long = READY

    inline fun submit(`else`: () -> Unit = { }, block: () -> Unit) {
        synchronized(this) {
            if (requested != CLOSED)
                try {
                    block()
                } catch (ex: Throwable) {
                    error(ex)
                }
            else
                `else`()
        }
    }

    protected abstract fun produce()

    override fun request(n: Long) {
        require(n >= 0) { "n must be positive!" }

        submit {
            val old = requested
            requested += n

            if (old != 0L) return

            while (buffer.isNotEmpty() && requested > 0)
                buffer.poll().yield()

            if (requested > 0)
                produce()
        }
    }

    override fun cancel() = submit {
        buffer.clear()
        requested = CLOSED
    }

    fun subscribe() = submit {
        if (requested == READY) {
            requested = 0
            downstream.onSubscribe(this)
        }
    }

    fun T.yield() = submit {
        if (requested == 0L)
            buffer.add(this)
        else {
            requested--
            downstream.onNext(this)
        }
    }

    fun error(ex: Throwable) = submit {
        cancel()

        downstream.onError(ex)
    }

    fun complete() = submit {
        cancel()

        downstream.onComplete()
    }
}