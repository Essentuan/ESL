package net.essentuan.esl.future

import net.essentuan.esl.Result
import net.essentuan.esl.fail
import net.essentuan.esl.ifPresentOrElse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import net.essentuan.esl.coroutines.delay
import net.essentuan.esl.coroutines.launch
import net.essentuan.esl.future.api.Future
import net.essentuan.esl.iteration.extensions.iterate
import net.essentuan.esl.other.stacktrace
import net.essentuan.esl.rx.ISubscription
import net.essentuan.esl.rx.RxState
import net.essentuan.esl.time.duration.Duration
import org.reactivestreams.Subscriber
import net.essentuan.esl.result
import java.util.LinkedList
import java.util.Queue
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
    final override var state: Future.State = Future.State.PENDING
        private set

    val stack: Queue<Consumer<Result<T>>> = LinkedList()
    val handlers = mutableListOf<Job>()

    @Synchronized
    fun complete(result: Result<T>) {
        if (state != Future.State.PENDING || result is net.essentuan.esl.Result.Empty) return

        this.result = result
        this.state = if (result is Result.Value) Future.State.RESOLVED else Future.State.REJECTED

        handlers iterate {
            it.cancel()
        }

        for (consumer in stack) consumer.accept(result)
    }

    @Synchronized
    protected fun copy(other: Future<T>) {
        other.except { throwable: Throwable -> this.raise(throwable) }.then { value: T -> this.complete(value) }
    }

    protected open fun complete(value: T) {
        complete(Result.of(value))
    }

    protected open fun raise(ex: Throwable) {
        complete(Result.fail(ex))
    }

    @Synchronized
    protected open fun push(consumer: Consumer<Result<T>>) {
        if (state == Future.State.PENDING) synchronized(stack) {
            stack.add(consumer)
        }
        else consumer.accept(result!!)
    }

    protected open fun <U> stage(): AbstractFuture<U> {
        return Stage()
    }

    @Synchronized
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

    @Synchronized
    override fun threaded(): Future<T> {
        val future = Threaded<T>()

        push { result: Result<T> -> future.complete(result) }

        return future
    }

    @Suppress("UNCHECKED_CAST")
    override fun <U> map(mapper: Function<T, U>): Future<U> {
        return branch {
            return@branch when (it) {
                is Result.Value -> Result.of(mapper.apply(it.value))
                else -> it as Result<U>
            }
        }
    }

    @Synchronized
    override fun <U> compose(mapper: (T) -> Future<U>): Future<U> {
        val stage = stage<U>()

        push {
            if (it is Result.Value) try {
                stage.copy(mapper(it.value))
            } catch (t: Throwable) {
                stage.raise(t)
            }
            else stage.complete(result as Result<U>)
        }

        return stage
    }

    override fun peek(value: Consumer<T>): Future<T> {
        return branch {
            if (it is Result.Value)
                value.accept(it.value)

            it
        }
    }

    override fun exec(runnable: Runnable): Future<T> {
        return branch { result: Result<T> ->
            runnable.run()
            result
        }
    }

    override fun except(handler: Consumer<Throwable>): Future<T> {
        return branch {
            if (it is Result.Fail)
                handler.accept(it.cause)

            it
        }
    }

    override fun handle(consumer: (Result<T>) -> Unit) {
        branch {
            consumer(it)

            it
        }
    }

    override fun revive(reviver: Function<Throwable, Result<T>>): Future<T> {
        return branch {
            if (it is Result.Fail) {
                val out = reviver.apply(it.cause)

                return@branch if (out is net.essentuan.esl.Result.Empty) it else out
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
    @OptIn(DelicateCoroutinesApi::class)
    override fun timeout(duration: Duration): Future<T> {
        handlers.add(launch {
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
        return requireNotNull(result) { "Cannot net.essentuan.esl.get net.essentuan.esl.result for pending future!" }
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
        @Synchronized
        override fun push(consumer: Consumer<Result<R>>) {
            this.stack.add(Consumer<Result<R>> { result: Result<R> ->
                ForkJoinPool.commonPool().execute { consumer.accept(result) }
            })
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
