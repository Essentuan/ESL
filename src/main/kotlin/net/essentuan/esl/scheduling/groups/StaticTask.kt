package net.essentuan.esl.scheduling.groups

import net.essentuan.esl.other.stacktrace
import net.essentuan.esl.reflections.Functions.Companion.static
import net.essentuan.esl.reflections.Reflections
import net.essentuan.esl.reflections.Types.Companion.objects
import net.essentuan.esl.reflections.extensions.extends
import net.essentuan.esl.reflections.extensions.get
import net.essentuan.esl.scheduling.Task
import net.essentuan.esl.scheduling.annotations.*
import net.essentuan.esl.scheduling.groups.weak.tasks
import net.essentuan.esl.scheduling.id
import net.essentuan.esl.scheduling.impl.GroupImpl
import net.essentuan.esl.scheduling.impl.TaskImpl
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.seconds
import java.util.*
import kotlin.random.Random
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend

abstract class StaticTask(
    id: String,
    rate: Duration,
    ttl: Duration = 10.seconds,
    capacity: Int = 1,
    immediate: Boolean = true,
    /**
     * Controls the number of times this task can run. When this value is -1 the task will run indefinitely.
     */
    private val runs: Int = -1
) : TaskImpl(id, rate, ttl, capacity, immediate) {
    private var executions = 0

    init {
        StaticTask += this
    }

    final override suspend fun invoke() {
        if (runs >= 0)
            synchronized(this) {
                if (executions++ >= runs) {
                    close(false)

                    return
                }
            }

        run()

        if (runs in 0..executions) {
            close(false)

            return
        }
    }

    abstract suspend fun run()

    override fun close(cancel: Boolean) {
        StaticTask -= this

        super.close(cancel)
    }

    private class FuncTask(
        private val executor: KFunction<*>
    ) : StaticTask(
        executor.id,
        executor[Every::class]!!.duration(),
        executor[Lifetime::class]?.duration() ?: 10.seconds,
        executor[Workers::class]?.value ?: 1
    ) {
        override suspend fun run() {
            executor.callSuspend()
        }
    }

    companion object : GroupImpl("static") {
        init {
            Reflections.functions
                .annotatedWith(Every::class)
                .static()
                .filter { it.parameters.isEmpty() }
                .forEach {
                    FuncTask(it)
                }

            Reflections.types
                .objects()
                .filterNot { it extends Companion::class }
                .forEach {
                    if (it.isAutoLoaded)
                        it.tasks.resume()
                }
        }
    }

    @JvmInline
    value class Builder(val id: String) {
        inline fun every(
            rate: Duration,
            ttl: Duration = 10.seconds,
            capacity: Int = 1,
            immediate: Boolean = true,
            runs: Int = -1,
            start: Boolean = true,
            crossinline block: suspend () -> Unit
        ): Task {
            return object : StaticTask(name, rate, ttl, capacity, immediate, runs) {
                init {
                    if (start)
                        resume()
                }

                override suspend fun run() =
                    block()
            }
        }

        inline fun after(
            rate: Duration,
            ttl: Duration = 10.seconds,
            crossinline block: suspend () -> Unit
        ): Task = every(rate, ttl, runs = 1, block = block)
    }
}

@OptIn(ExperimentalStdlibApi::class)
inline fun schedule(name: String, block: StaticTask.Builder.() -> Task): Task {
    return StaticTask.Builder(
        "$name${
            (stacktrace().contentHashCode() * 31 + Random.nextInt()).toHexString()
        }."
    ).run(block)
}
