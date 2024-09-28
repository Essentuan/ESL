package net.essentuan.esl.scheduling

import net.essentuan.esl.Result
import net.essentuan.esl.future.api.Future
import net.essentuan.esl.reflections.extensions.classOf
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.extensions.timeSince
import java.io.Closeable
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.CompletionException
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

interface Task : Set<Task.Worker>, Closeable {
    val id: String
    val rate: Duration
    val ttl: Duration
    val capacity: Int

    val isSuspended: Boolean
    val isReady: Boolean

    fun spin(context: Context): Worker

    fun resume()

    fun suspend(cancel: Boolean = false)

    fun close(cancel: Boolean)

    override fun close() = close(true)

    class Exception(val process: Task.Process, cause: Throwable) : CompletionException(cause)

    sealed interface Process : List<Process>, TaskScope {
        val id: String
        val name: String

        val task: Task
        val status: Int

        val isCompleted: Boolean
    }

    sealed interface Context : Process {
        fun start(block: suspend TaskScope.() -> Unit): Future<Unit>

        fun cancel()
    }

    interface Worker : List<Process> {
        val id: Int
        val created: Date

        val age: Duration
            get() =
                created.timeSince()

        val status: Int

        fun cancel()
    }

    interface Group : Set<Task>, Closeable {
        val name: String

        operator fun plusAssign(task: Task)

        operator fun get(id: String): Task?

        operator fun contains(id: String): Boolean

        override fun contains(element: Task): Boolean =
            contains(element.id)

        operator fun minusAssign(task: Task)

        fun resume()

        fun suspend(cancel: Boolean = false)
    }

    companion object {
        const val PENDING = 0
        const val ENQUEUED = 1
        const val RUNNING = 2
        const val SUSPENDED = 3
        const val COMPLETED = 4
        const val CANCELLED = 5
    }
}

val Method.id: String
    get() = "$name\$${
        arrayOf(
            declaringClass,
            name,
            genericParameterTypes.map { it.classOf() }
        ).contentDeepHashCode()
    }"

val KFunction<*>.id: String
    get() = javaMethod!!.id