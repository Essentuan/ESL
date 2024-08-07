package net.essentuan.esl.future.api

import net.essentuan.esl.Result
import net.essentuan.esl.future.AbstractCompletable
import net.essentuan.esl.future.AbstractFuture
import net.essentuan.esl.future.LazyFuture
import net.essentuan.esl.reflections.extensions.extends
import net.essentuan.esl.time.TimeUnit
import net.essentuan.esl.time.duration.Duration
import org.reactivestreams.Publisher
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeoutException
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import kotlin.reflect.KClass

interface Future<T> : Publisher<T> {
    val pending: Boolean
        get() = state == State.PENDING

    val state: State

    val stacktrace: Array<StackTraceElement>

    fun threaded(): Future<T>

    fun <U> map(mapper: Function<T, U>): Future<U>

    fun <U> compose(mapper: (T) -> Future<U>): Future<U>

    fun peek(value: Consumer<T>): Future<T>

    fun exec(runnable: Runnable): Future<T>

    fun except(handler: Consumer<Throwable>): Future<T>

    @Suppress("UNCHECKED_CAST")
    fun <U : Throwable> except(handler: Consumer<U>, cls: Class<U>): Future<T> {
        return except { t: Throwable ->
            if (cls.isAssignableFrom(t.javaClass))
                handler.accept(t as U)
        }
    }

    fun <U : Throwable> except(handler: Consumer<U>, cls: KClass<U>): Future<T> {
        return except(handler, cls.java)
    }

    fun revive(reviver: Function<Throwable, Result<T>>): Future<T>

    fun revive(cls: Class<out Throwable>, supplier: Supplier<Result<T>>): Future<T> {
        return revive { t: Throwable ->
            if (t.javaClass extends cls)
                return@revive supplier.get()

            Result.empty()
        }
    }

    fun revive(value: T, cls: Class<out Throwable>): Future<T> {
        return revive { t: Throwable ->
            if (cls.isAssignableFrom(t.javaClass)) return@revive Result.of(value)

            Result.empty()
        }
    }

    fun revive(value: T, cls: KClass<out Throwable>): Future<T> {
        return revive(value, cls.java)
    }

    fun then(consumer: (T) -> Unit): Future<T>

    fun handle(consumer: (Result<T>) -> Unit)

    fun finish(): Future<Unit> {
        return map { }
    }

    fun timeout(duration: Duration): Future<T>

    fun timeout(length: Double, unit: TimeUnit): Future<T> {
        return timeout(Duration(length, unit))
    }

    suspend fun await(): T

    fun result(): Result<T>

    fun toCompletable(): CompletableFuture<T> = CompletableFuture<T>().apply {
        this@Future.except(this::completeExceptionally).then(this::complete)
    }

    enum class State {
        PENDING,
        RESOLVED,
        REJECTED
    }

    companion object {
        operator fun <T> invoke(value: T): Future<T> = Completable(value)

        operator fun <T> invoke(stage: CompletionStage<T>): Future<T> = AbstractFuture.Native(stage)

        inline operator fun <T> invoke(crossinline block: suspend () -> T): Future<T> = Completable(block)

        fun <T> lazy(supplier: Supplier<Future<T>>): Future<T> {
            return LazyFuture(supplier)
        }

        fun sleep(duration: Duration): Future<Unit> {
            return Completable<Unit>()
                .timeout(duration)
                .revive(Unit, TimeoutException::class.java)
        }

        fun sleep(length: Double, unit: TimeUnit): Future<Unit> {
            return sleep(Duration(length, unit))
        }

        fun <T> completed(): Future<T> {
            return AbstractCompletable.COMPLETED as Future<T>
        }
    }
}

fun <T> CompletionStage<T>.esl(): Future<T> = Future.invoke(this)

class CompletionException(
    future: Future<*>,
    message: String? = null,
    cause: Throwable?
) : RuntimeException(
    "${if (message == null) "" else "$message | "}Created By",
    cause,
    false,
    true
) {
    init {
        stackTrace = future.stacktrace
    }
}