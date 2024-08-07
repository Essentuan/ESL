package net.essentuan.esl.rx

import net.essentuan.esl.other.lock
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.LinkedList
import java.util.Queue
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

private val RESUME_RESULT = Result.success(Unit)
private val COMPLETED = Throwable()

class BatchedIterator<T>(override var batchSize: Long) : RxIterator<T>, Subscriber<T> {
    val queue: Queue<T> = LinkedList()

    lateinit var subscription: Subscription
    var completion: Throwable? = null
    var signal = mutableListOf<Continuation<Unit>>()

    val closed: Boolean
        get() = completion != null

    fun request() {
        if (lock { queue.size < batchSize / 2 && !closed })
            subscription.request(batchSize)
    }

    suspend fun wait(): Unit = suspendCoroutine {
        request()

        if (synchronized(this@BatchedIterator) {
                if (queue.isNotEmpty() || closed)
                    return@synchronized true
                else
                    signal += it

                false
            })
            it.resumeWith(RESUME_RESULT)
    }

    fun resume() {
        val old = signal
        signal = mutableListOf()

        old.forEach { it.resumeWith(RESUME_RESULT) }
    }

    override suspend fun hasNext(): Boolean {
        wait()

        synchronized(this) {
            if (closed && completion != COMPLETED)
                throw completion!!

            return queue.isNotEmpty()
        }
    }


    override fun next(): T {
        return lock {
            if (closed && completion != COMPLETED)
                throw completion!!

            if (queue.isEmpty())
                throw NoSuchElementException()

            queue.poll()
        }.also { request() }
    }

    override fun onSubscribe(s: Subscription) {
        lock { subscription = s }
        request()
    }

    @Synchronized
    override fun onError(t: Throwable) {
        completion = t
        resume()
    }

    @Synchronized
    override fun onComplete() {
        completion = COMPLETED
        resume()
    }

    @Synchronized
    override fun onNext(t: T) {
        queue.offer(t)
        resume()
    }
}