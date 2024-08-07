package net.essentuan.esl.rx

import net.essentuan.esl.other.lock
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import java.util.LinkedList
import java.util.Queue
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

abstract class Generator<T>(
    downstream: Subscriber<in T>
) : Producer<T>(downstream), PublisherScope<T>, Continuation<Unit>, Subscriber<T> {
    var ret: Waiting? = null
        set(value) {
            field?.backing?.resume(Unit)
            field = value
            value?.publisher?.subscribe(this)
        }
    var stack: Queue<Continuation<Unit>> = LinkedList()

    init {
        @Suppress("LeakingThis")
        stack.add(::generate.createCoroutine(this))
    }

    abstract suspend fun generate()

    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun produce() {
        if (ret != null) {
            ret!!.subscription.request(requested)
            return
        }

        while (stack.lock { isNotEmpty() }) {
            submit {
                if (requested <= 0)
                    return

                if (ret != null) {
                    ret!!.subscription.request(requested)
                    return
                }

                @Suppress("UNCHECKED_CAST")
                when (val cont = stack.poll()) {
                    is Generator<*>.Waiting -> {
                        ret = cont as Generator<T>.Waiting
                        return
                    }

                    is Generator<*>.Value -> {
                        (cont.value as T).yield()
                        cont.backing.resume(Unit)
                    }

                    else -> cont?.resume(Unit)
                }
            }
        }
    }

    override suspend fun yield(value: T) {
        submit {
            if (requested > 0 && ret == null) {
                value.yield()
                return
            }
        }

        return suspendCoroutine { Value(it, value) }
    }

    override suspend fun yieldAll(value: Publisher<T>) {
        return suspendCoroutine { Waiting(it, value) }
    }

    override fun resumeWith(result: Result<Unit>) {
        if (result.isFailure)
            error(result.exceptionOrNull()!!)
        else
            complete()
    }

    override fun cancel() = submit {
        super.cancel()

        while (stack.isNotEmpty())
            stack.poll().resumeWithException(CancellationException())
    }

    override fun onSubscribe(s: ISubscription) {
        ret!!.subscription = s

        if (requested > 0)
            s.request(requested)
    }

    override fun onError(ex: Throwable) = error(ex)

    override fun onComplete() {
        ret = null

        if (requested > 0)
            produce()
    }

    override fun onNext(value: T) = value.yield()

    inner class Value(val backing: Continuation<Unit>, val value: T) : Continuation<Unit> {
        init {
            submit({ backing.resumeWithException(CancellationException()) }) {
                if (requested > 0 && ret == null)
                    value.yield()
                else
                    stack.add(this)
            }
        }

        override val context: CoroutineContext
            get() = backing.context

        override fun resumeWith(result: Result<Unit>) = backing.resumeWith(result)
    }

    inner class Waiting(val backing: Continuation<Unit>, val publisher: Publisher<T>) : Continuation<Unit> {
        lateinit var subscription: ISubscription

        init {
            submit({ backing.resumeWithException(CancellationException()) }) {
                if (ret == null && stack.isEmpty())
                    ret = this
                else
                    stack.add(this)
            }
        }

        override val context: CoroutineContext
            get() = backing.context

        override fun resumeWith(result: Result<Unit>) = backing.resumeWith(result)
    }
}