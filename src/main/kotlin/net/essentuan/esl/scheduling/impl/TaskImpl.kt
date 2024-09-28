package net.essentuan.esl.scheduling.impl

import kotlinx.coroutines.CancellationException
import net.essentuan.esl.scheduling.Scheduler
import net.essentuan.esl.scheduling.Task
import net.essentuan.esl.scheduling.TaskScope
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.seconds
import net.essentuan.esl.time.extensions.timeSince
import net.essentuan.esl.Result
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class TaskImpl(
    final override val id: String,
    override val rate: Duration,
    override val ttl: Duration = 10.seconds,
    final override val capacity: Int = 1,
    immediate: Boolean = true
) : Task, suspend (TaskScope) -> Unit {
    init {
        require(capacity > 0)
    }

    private val workers = ConcurrentHashMap<Int, WorkerImpl>()
    private var lastExecution = if (immediate) Date(0) else Date()

    final override var isSuspended: Boolean = true
        private set

    override val isReady: Boolean
        @Synchronized get() = !isSuspended && workers.size < capacity && lastExecution.timeSince() >= rate

    final override fun spin(context: Task.Context): Task.Worker {
        lastExecution = Date()

        val worker = WorkerImpl(workers.size, context)
        context.start(this).handle(worker)

        return worker
    }

    @Synchronized
    override fun resume() {
        if (!isSuspended)
            return

        isSuspended = false
    }

    override fun suspend(cancel: Boolean) {
        synchronized(this) {
            if (isSuspended)
                return

            isSuspended = true
        }

        if (cancel)
            for (worker in this)
                worker.cancel()
    }

    override val size: Int
        get() = workers.size

    override fun isEmpty(): Boolean =
        workers.isEmpty()

    override fun contains(element: Task.Worker): Boolean =
        workers[element.id] == element

    override fun containsAll(elements: Collection<Task.Worker>): Boolean {
        for (e in elements)
            if (e !in this)
                return false

        return false
    }

    override fun iterator(): Iterator<Task.Worker> =
        workers.values.iterator()

    override fun close(cancel: Boolean) {
        suspend(cancel)
    }

    /**
     * Runs after a worker finishes its task.
     */
    protected open fun complete() = Unit

    private inner class WorkerImpl(
        override val id: Int,
        private val context: Task.Context
    ) : Task.Worker, List<Task.Process> by context, (Result<Unit>) -> Unit {
        init {
            workers[id] = this
        }

        override val created: Date = Date()
        override val status: Int
            get() = context.status

        override fun invoke(result: Result<Unit>) {
            workers.remove(id, this)
            complete()

            when (val ex = (result as? Result.Fail<*>)?.cause ?: return) {
                is CancellationException -> Unit
                is Task.Exception -> {
                    Scheduler.error(
                        "An exception was thrown while executing ${ex.process.id}!",
                        ex.cause
                    )
                }

                else ->
                    Scheduler.error(
                        "An exception was thrown while executing ${this@TaskImpl.id}#$id!",
                        result.cause
                    )
            }
        }

        override fun cancel() {
            workers.remove(id, this)
            context.cancel()
        }
    }
}