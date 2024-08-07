package net.essentuan.esl.scheduling.api

import net.essentuan.esl.future.api.Future
import net.essentuan.esl.other.stacktrace
import net.essentuan.esl.reflections.extensions.classOf
import net.essentuan.esl.scheduling.Static
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.seconds
import java.io.Closeable
import java.lang.reflect.Method
import java.util.Date
import kotlin.random.Random
import kotlin.reflect.KFunction

interface Task : Collection<Task.Worker>, Closeable {
    val id: String
    val rate: Duration
    val lifetime: Duration
    val capacity: Int

    val suspended: Boolean

    fun cull()

    fun ready(): Boolean

    fun spin(starter: Runnable, finisher: Runnable)

    fun resume()

    fun suspend(cancel: Boolean = false)

    fun close(cancel: Boolean)

    override fun close() = close(true)

    interface Worker {
        val id: Int
        val createdAt: Date
        val age: Duration

        val isComplete: Boolean

        fun marked(): Boolean

        fun cancel()
    }

    interface Group : Collection<Task>, Closeable {
        val name: String

        operator fun get(uid: String): Task?

        operator fun get(method: Method): Task?

        operator fun get(func: KFunction<*>): Task?

        operator fun contains(uid: String): Boolean

        operator fun contains(method: Method): Boolean

        operator fun contains(func: KFunction<*>): Boolean

        fun resume()

        fun suspend(cancel: Boolean = false)
    }

    @JvmInline
    value class Builder internal constructor(val func: suspend () -> Unit) {
        infix fun every(rate: Duration): Task = every(rate, capacity = 1)

        fun every(
            rate: Duration,
            lifetime: Duration = 10.seconds,
            capacity: Int = 1
        ): Task = Static.Task(
            "lambda-${stacktrace().contentHashCode()}#${Random.nextInt(0, 10000)}",
            rate,
            lifetime,
            capacity,
            func
        )

        infix fun after(rate: Duration): Future<Unit> = after(rate, lifetime = 10.seconds)

        fun after(
            rate: Duration,
            lifetime: Duration = 10.seconds
        ): Future<Unit> = Static.Scheduled(
            "lambda-${stacktrace().contentHashCode()}#${Random.nextInt(0, 10000)}",
            rate,
            lifetime,
            func
        ).apply { resume() }
    }
}

fun schedule(func: suspend () -> Unit): Task.Builder = Task.Builder(func)

fun Method.id(): String = "$name\$${
    arrayOf(
        declaringClass,
        name,
        genericParameterTypes.map { it.classOf() }
    ).contentDeepHashCode()
}"