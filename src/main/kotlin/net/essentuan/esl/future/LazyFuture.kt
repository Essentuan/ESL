package net.essentuan.esl.future

import net.essentuan.esl.Result
import net.essentuan.esl.future.api.Future
import net.essentuan.esl.other.stacktrace
import net.essentuan.esl.time.duration.Duration
import org.reactivestreams.Subscriber
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

open class LazyFuture<T>(val supplier: Supplier<Future<T>>) : Future<T> {
    override val stacktrace: Array<StackTraceElement> = stacktrace()

    private var future: Future<T>? = null

    fun future(): Future<T> {
        if (future == null)
            future = supplier.get()

        return future!!
    }

    override val state: Future.State
        get() = future().state

    override fun threaded(): Future<T> {
        return future().threaded()
    }

    override fun exec(runnable: Runnable): Future<T> {
        return future().exec(runnable)
    }

    override fun except(handler: Consumer<Throwable>): Future<T> {
        return future().except(handler)
    }

    override fun handle(consumer: (Result<T>) -> Unit) {
        future().handle(consumer)
    }

    override fun timeout(duration: Duration): Future<T> {
        return future().timeout(duration)
    }

    override suspend fun await(): T {
        return future().await()
    }

    override fun result(): Result<T> {
        return future().result()
    }

    override fun subscribe(s: Subscriber<in T>) {
        future().subscribe(s)
    }

    override fun then(consumer: (T) -> Unit): Future<T> {
        return future().then(consumer)
    }

    override fun revive(reviver: Function<Throwable, Result<T>>): Future<T> {
        return future().revive(reviver)
    }

    override fun peek(value: Consumer<T>): Future<T> {
        return future().peek(value)
    }

    override fun <U> compose(mapper: (T) -> Future<U>): Future<U> {
        return future().compose(mapper)
    }

    override fun <U> map(mapper: Function<T, U>): Future<U> {
        return future().map(mapper)
    }
}
