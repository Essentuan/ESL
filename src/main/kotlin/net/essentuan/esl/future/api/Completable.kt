package net.essentuan.esl.future.api

import net.essentuan.esl.future.AbstractCompletable
import java.util.function.Supplier
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.startCoroutine

interface Completable<T> : Future<T> {
    fun complete(value: T)

    fun complete(supplier: Supplier<T>) {
        complete(supplier.get())
    }

    infix fun raise(ex: Throwable)

    infix fun raise(supplier: Supplier<Throwable>) {
        raise(supplier.get())
    }

    fun cancel() {
        raise(CancellationException())
    }

    fun from(other: Future<T>): Completable<T>

    companion object {
        operator fun <T> invoke(): Completable<T> {
            return object : AbstractCompletable<T>() {}
        }

        inline operator fun <T> invoke(crossinline block: suspend () -> T): Completable<T> {
            return object : AbstractCompletable<T>(), Continuation<T> {
                override val context: CoroutineContext
                    get() = EmptyCoroutineContext

                init {
                    ::start.startCoroutine(this)
                }

                suspend fun start() = block()

                override fun resumeWith(result: Result<T>) {
                    when {
                        result.isFailure -> raise(result.exceptionOrNull()!!)
                        else -> complete(result.getOrThrow())
                    }
                }
            }
        }

        operator fun <T> invoke(value: T): Completable<T> {
            val future = Completable<T>()
            future.complete(value)

            return future
        }
    }
}