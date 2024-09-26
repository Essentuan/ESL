package net.essentuan.esl.scheduling

import kotlinx.coroutines.CoroutineScope
import net.essentuan.esl.coroutines.launch
import net.essentuan.esl.future.api.Future
import net.essentuan.esl.other.stacktrace
import net.essentuan.esl.reflections.extensions.classOf
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.seconds
import net.essentuan.esl.time.extensions.timeSince
import java.io.Closeable
import java.lang.reflect.Method
import java.util.Date
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
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

    enum class State {
        PENDING,
        RUNNING,
        SUSPENDED,
        COMPLETED,
        CANCELLED;
    }

    sealed interface Context : CoroutineContext.Element {
        override val key: CoroutineContext.Key<*>
            get() = Task

        val task: Task
        val state: State

        val isCompleted: Boolean

        fun start(block: suspend () -> Unit, completion: (Result<Unit>) -> Unit)

        fun cancel()
    }

    interface Worker {
        val id: Int
        val created: Date

        val age: Duration
            get() =
                created.timeSince()

        val state: State

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

    companion object : CoroutineContext.Key<Context>
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