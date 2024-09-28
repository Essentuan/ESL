package net.essentuan.esl.future.api

import net.essentuan.esl.Result
import net.essentuan.esl.future.AbstractCompletable
import net.essentuan.esl.future.AbstractFuture
import net.essentuan.esl.isFail
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
        get() = state == PENDING

    val state: Int

    val stacktrace: Array<StackTraceElement>

    fun result(): Result<T>

    fun threaded(): Future<T>

    infix fun <U> map(mapper: (T) -> U): Future<U>

    infix fun <U> compose(mapper: (T) -> Future<U>): Future<U>

    infix fun peek(value: (T) -> Unit): Future<T>

    infix fun exec(runnable: () -> Unit): Future<T>

    fun revive(reviver: (Throwable) -> Result<T>): Future<T>

    fun revive(cls: Class<out Throwable>, supplier: () -> Result<T>): Future<T> {
        return revive {
            if (it.javaClass extends cls)
                supplier()
            else
                Result.empty()
        }
    }

    fun revive(value: T, cls: Class<out Throwable>): Future<T> {
        return revive {
            if (it.javaClass extends cls)
                Result.of(value)
            else
                Result.empty()
        }
    }

    fun revive(value: T, cls: KClass<out Throwable>): Future<T> {
        return revive(value, cls.java)
    }

    infix fun then(consumer: (T) -> Unit): Future<T>

    infix fun handle(consumer: (Result<T>) -> Unit): Future<T>

    fun finish(): Future<Unit> {
        return map { }
    }

    fun timeout(duration: Duration): Future<T>

    fun timeout(length: Double, unit: TimeUnit): Future<T> {
        return timeout(Duration(length, unit))
    }

    suspend fun await(): T

    fun toCompletable(): CompletableFuture<T> = CompletableFuture<T>().apply {
        this@Future.except(this::completeExceptionally).then(this::complete)
    }

    companion object {
        const val PENDING = 0
        const val RESOLVED = 1
        const val REJECTED = 2

        operator fun <T> invoke(value: T): Future<T> = Completable(value)

        operator fun <T> invoke(stage: CompletionStage<T>): Future<T> = AbstractFuture.Native(stage)

        inline operator fun <T> invoke(crossinline block: suspend () -> T): Future<T> = Completable(block)

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

inline infix fun <T, reified EX : Throwable> Future<T>.except(crossinline block: (EX) -> Unit): Future<T> {
    return handle {
        if (it.isFail() && it.cause is EX)
            block(it.cause)
    }
}

fun <T> CompletionStage<T>.toFuture(): Future<T> = Future.invoke(this)

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