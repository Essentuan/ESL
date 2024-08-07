package net.essentuan.esl.async.pipeline

import net.essentuan.esl.optional.Optional
import net.essentuan.esl.optional.extensions.opt
import net.essentuan.esl.other.unsupported
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.function.Consumer

abstract class Gatherer<In, Out> protected constructor() : Publisher<Out> {
    private var publisher: Publisher<In>? = null

    private var subscription: Sub? = null
    private var subscriber: Middleman? = null

    private val errorHandlers: MutableList<Consumer<Throwable>> = ArrayList()

    fun bind(publisher: Publisher<In>): Gatherer<In, Out> {
        if (this.publisher != null)
            unsupported("Cannot bind to already bound gatherer!")

        this.publisher = publisher

        return this
    }

    protected open fun onRequest(value: Long): Long {
        return value
    }

    protected open fun onNext(value: In) {
        accept(value as Out)
    }

    protected open fun prepare() {
    }

    protected fun accept(result: Out) {
        subscriber!!.base.onNext(result)
    }

    protected fun error(throwable: Throwable) {
        onError(throwable).ifPresent {
            errorHandlers.forEach { c -> c.accept(it) }
            cancel()
        }
    }

    protected fun onError(throwable: Throwable): Optional<Throwable> {
        return throwable.opt()
    }

    fun onError(handler: Consumer<Throwable>): Gatherer<In, Out> {
        errorHandlers.add(handler)

        return this
    }

    protected open fun onComplete() {
        complete()
    }

    protected fun complete() {
        if (isClosed) return

        try {
            subscriber!!.base.onComplete()

            cancel()
        } catch (t: Throwable) {
            onError(t)
        }
    }

    protected open fun onCancel() {
    }

    val isClosed: Boolean
        get() = subscription == null

    fun close() {
        if (isClosed) return

        subscription?.cancel()
    }

    private fun cancel() {
        subscription = null
        subscriber = null
    }

    override fun subscribe(s: Subscriber<in Out>) {
        if (publisher == null) unsupported("Cannot subscribe to unbound gatherer!")

        subscriber = Middleman(s)
        publisher!!.subscribe(subscriber)
    }

    fun pipe(): Pipeline<Out> = RefPipeline(this)

    private inner class Sub(private val base: Subscription) : Subscription {
        override fun request(n: Long) {
            var n = n
            if (n < 0)
                n = Int.MAX_VALUE.toLong()

            prepare()
            base.request(onRequest(n))
        }

        override fun cancel() {
            if (!isClosed) {
                try {
                    onCancel()
                    base.cancel()

                    if (!isClosed) {
                        subscriber!!.base.onComplete()
                        this@Gatherer.cancel()
                    }
                } catch (t: Throwable) {
                    subscriber!!.onError(t)
                }
            }
        }
    }

    private inner class Middleman(val base: Subscriber<in Out>) :
        Subscriber<In> {
        init {
            this@Gatherer.onError { throwable: Throwable? -> base.onError(throwable) }
        }

        override fun onSubscribe(s: Subscription) {
            try {
                subscription = Sub(s)
                base.onSubscribe(subscription)
            } catch (t: Throwable) {
                error(t)
            }
        }

        override fun onNext(value: In) {
            try {
                if (isClosed) return

                this@Gatherer.onNext(value)
            } catch (t: Throwable) {
                error(t)
            }
        }

        override fun onError(throwable: Throwable) {
            error(throwable)
        }

        override fun onComplete() {
            if (isClosed) return

            try {
                this@Gatherer.onComplete()
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    companion object {
        @JvmStatic
        fun <T> of(publisher: Publisher<T>): Gatherer<T, T> {
            return object : Gatherer<T, T>() {}.bind(publisher)
        }
    }
}
