package net.essentuan.esl.future

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import net.essentuan.esl.Result
import net.essentuan.esl.coroutines.delay
import net.essentuan.esl.coroutines.dispatch
import net.essentuan.esl.coroutines.launch
import net.essentuan.esl.fail
import net.essentuan.esl.future.api.Future
import net.essentuan.esl.future.api.except
import net.essentuan.esl.ifPresentOrElse
import net.essentuan.esl.other.stacktrace
import net.essentuan.esl.result
import net.essentuan.esl.rx.ISubscription
import net.essentuan.esl.rx.RxState
import net.essentuan.esl.time.duration.Duration
import org.reactivestreams.Subscriber
import java.util.LinkedList
import java.util.concurrent.CompletionStage
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeoutException
import java.util.function.Consumer
import java.util.function.Function
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

abstract class AbstractFuture<T> protected constructor(override val stacktrace: Array<StackTraceElement> = stacktrace()) :
    Future<T> {
    var result: Result<T>? = null

    final override var state: Int = Future.PENDING
        private set

    val stack = LinkedList<Consumer<Result<T>>>()
    val handlers = LinkedList<Future<*>>()

    open fun complete(result: Result<T>) {
        synchronized(this) {
            if (state != Future.PENDING || result is Result.Empty) return

            this.result = result
            state = if (result is Result.Value) Future.RESOLVED else Future.REJECTED
        }

        handlers.clear()

        while (stack.isNotEmpty())
            stack.poll().accept(result)
    }

    protected fun copy(other: Future<T>) {
        other.except { throwable: Throwable -> this.raise(throwable) }.then { value: T -> this.complete(value) }
    }

    protected open fun complete(value: T) {
        complete(Result.of(value))
    }

    protected open fun raise(ex: Throwable) {
        complete(Result.fail(ex))
    }

    protected open fun push(consumer: Consumer<Result<T>>) {
        synchronized(this) {
            if (state == Future.PENDING) {
                stack.add(consumer)

                return
            }
        }

        consumer.accept(result!!)
    }

    protected open fun <U> stage(): AbstractFuture<U> {
        return Stage()
    }

    protected fun <U> branch(handler: Function<Result<T>, Result<U>>): Future<U> {
        val stage = stage<U>()

        push {
            try {
                stage.complete(handler.apply(it))
            } catch (t: Throwable) {
                stage.raise(t)
            }
        }

        return stage
    }

    override fun threaded(): Future<T> {
        val future = Threaded<T>()

        push { result: Result<T> -> future.complete(result) }

        return future
    }

    @Suppress("UNCHECKED_CAST")
    override fun <U> map(mapper: (T) -> U): Future<U> {
        return branch {
            return@branch when (it) {
                is Result.Value -> Result.of(mapper(it.value))
                else -> it as Result<U>
            }
        }
    }

    override fun <U> compose(mapper: (T) -> Future<U>): Future<U> {
        val stage = stage<U>()

        push {
            @Suppress("UNCHECKED_CAST")
            if (it is Result.Value) try {
                stage.copy(mapper(it.value))
            } catch (t: Throwable) {
                stage.raise(t)
            }
            else stage.complete(result as Result<U>)
        }

        return stage
    }

    override fun peek(value: (T) -> Unit): Future<T> {
        return branch {
            if (it is Result.Value)
                value(it.value)

            it
        }
    }

    override fun exec(runnable: () -> Unit): Future<T> {
        return branch { result: Result<T> ->
            runnable()
            result
        }
    }

    override fun handle(consumer: (Result<T>) -> Unit): Future<T> {
        return branch {
            consumer(it)

            it
        }
    }

    override fun revive(reviver: (Throwable) -> Result<T>): Future<T> {
        return branch {
            if (it is Result.Fail) {
                val out = reviver(it.cause)

                return@branch if (out is Result.Empty) it else out
            }

            it
        }
    }

    override fun then(consumer: (T) -> Unit): Future<T> {
        return branch {
            if (it is Result.Value)
                consumer(it.value)

            it
        }
    }

    @Synchronized
    override fun timeout(duration: Duration): Future<T> {
        if (state != Future.PENDING)
            return this

        handlers.add(dispatch {
            delay(duration)

            raise(TimeoutException())
        })

        return this
    }

    override suspend fun await(): T = suspendCancellableCoroutine { cont ->
        push {
            when (it) {
                is Result.Value -> cont.resume(it.value)
                is Result.Fail -> cont.resumeWithException(it.cause)
                else -> cont.resumeWithException(NoSuchElementException())
            }
        }
    }

    @Synchronized
    override fun result(): Result<T> {
        return result ?: Result.empty()
    }

    override fun subscribe(s: Subscriber<in T>) {
        Subscription(s)
    }

    private inner class Subscription(val downstream: Subscriber<in T>) : ISubscription {
        var state = RxState.READY

        init {
            downstream.onSubscribe(this)
        }

        override fun request(n: Long) {
            require(n >= 0) { "n must be positive!" }

            if (state == RxState.READY && n > 0) {
                handle {
                    it.ifPresentOrElse({ e ->
                        downstream.onNext(e)
                        downstream.onComplete()
                    }) { ex ->
                        downstream.onError(ex ?: NoSuchElementException())
                    }
                }

                state = RxState.CLOSED
            }
        }

        override fun cancel() {
            state = RxState.CLOSED
        }
    }

    private inner class Stage<U> : AbstractFuture<U>(stacktrace)

    private inner class Threaded<R> : AbstractFuture<R>(stacktrace) {
        override fun push(consumer: Consumer<Result<R>>) {
            synchronized(this) {
                if (state == Future.PENDING) {
                    stack.add {
                        ForkJoinPool.commonPool().execute {
                            consumer.accept(it)
                        }
                    }

                    return
                }
            }

            ForkJoinPool.commonPool().execute {
                consumer.accept(result!!)
            }
        }

        override fun <U> stage(): AbstractFuture<U> {
            return Threaded()
        }

        override fun threaded(): Future<R> {
            return this
        }
    }

    class Native<T>(stage: CompletionStage<T>) : AbstractFuture<T>() {
        init {
            stage.whenComplete { value: T, throwable: Throwable? -> if (throwable != null) throwable.fail() else value.result() }
        }
    }
}
