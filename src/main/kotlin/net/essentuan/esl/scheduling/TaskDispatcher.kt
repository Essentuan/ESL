package net.essentuan.esl.scheduling

import kotlinx.coroutines.*
import net.essentuan.esl.KResult
import net.essentuan.esl.Result
import net.essentuan.esl.coroutines.launch
import net.essentuan.esl.future.AbstractFuture
import net.essentuan.esl.future.api.Future
import net.essentuan.esl.future.api.except
import net.essentuan.esl.isFail
import net.essentuan.esl.time.duration.FormatFlag
import net.essentuan.esl.toResult
import java.util.*
import java.util.concurrent.CompletionException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.startCoroutine

private typealias Work = Pair<CoroutineContext, Runnable>

internal object TaskDispatcher : CoroutineDispatcher() {
    const val COLD = 0
    const val HOT = 1

    private var state: Int = COLD
        @Synchronized
        private set

    var capacity: Int = 5
        @Synchronized
        set(value) {
            require(state == COLD) {
                "Cannot set capacity of the task pool after it has started!"
            }

            field = value
        }

    private val threadGroup = ThreadGroup("scheduler")
    private val workers = mutableListOf<Worker>()

    private val queue = LinkedBlockingQueue<Work>()

    @Synchronized
    fun start() {
        require(state == COLD) { "The task pool has already started!" }

        thread("coordinator") {
            while (true) {
                try {
                    for (group in Scheduler) {
                        for (task in group) {
                            if (task.isReady) {
                                val context = ContextImpl(task)
                                context.worker = task.spin(context)
                            }
                        }
                    }

                    Thread.sleep(1)
                } catch (ex: InterruptedException) {
                    //Ignored
                } catch (ex: Exception) {
                    Scheduler.error("An exception has been thrown in the coordinator!", ex)
                }
            }
        }

        for (i in 1..capacity)
            workers += Worker(i)

        thread("executioner") {
            while (true) {
                try {
                    for (group in Scheduler) {
                        for (task in group) {
                            for (worker in task) {
                                if (worker.age > task.ttl) {
                                    worker.cancel()

                                    Scheduler.error(
                                        "${task.id}#${worker.id} has exceeded its max age! It has been cancelled after ${
                                            worker.age.print(
                                                FormatFlag.COMPACT
                                            )
                                        }."
                                    )
                                }
                            }
                        }
                    }

                    Thread.sleep(20)
                } catch (ex: InterruptedException) {
                    //Ignored
                } catch (ex: Exception) {
                    Scheduler.error("An exception has been thrown in the execution!", ex)
                }
            }
        }
    }

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        val task = requireNotNull(context[ProcessImpl]) { "Only tasks can be submitted to TaskDispatcher!" }

        if (!task.isCompleted)
            queue.offer(context to block)
    }

    private inline fun thread(name: String, crossinline block: Thread.() -> Unit) {
        object : Thread(threadGroup, name) {
            init {
                isDaemon = true

                start()
            }

            override fun run() {
                block()
            }
        }
    }

    private abstract class ProcessImpl<T>(
        final override val task: Task,
        var thread: Worker? = null,
        private val children: MutableList<ProcessImpl<*>> = CopyOnWriteArrayList()
    ) : CoroutineContext.Element, Continuation<T>, AbstractFuture<T>(), Task.Process, List<Task.Process> by children {
        private var count = 0
        private var ret: Result<T>? = null

        override val key: CoroutineContext.Key<*>
            get() = ProcessImpl

        final override val context: CoroutineContext
            get() = TaskDispatcher + this

        override var status: Int = Task.PENDING
            get() = when (val result = result()) {
                is Result.Value<*> -> Task.COMPLETED
                is Result.Fail<*> -> if (result.cause is CancellationException) Task.CANCELLED else Task.COMPLETED
                else -> field
            }

        override val isCompleted: Boolean
            get() = result() !is Result.Empty

        override fun complete(result: Result<T>) {
            if (result !is Result.Fail<*>)
                synchronized(this) {
                    if (ret == null || count > 0)
                        return
                }

            super.complete(result)
        }

        public override fun raise(ex: Throwable) {
            super.raise(ex)
        }

        override fun resumeWith(result: KResult<T>) {
            ret = result.toResult()

            complete(result.toResult())
        }

        final override fun <T> launch(name: String, block: suspend TaskScope.() -> T): Future<T> {
            return synchronized(this) {
                count++
                val child = ChildProcess<T>(name, this)
                children += child

                child.handle {
                    synchronized(this) {
                        count--
                    }

                    if (it.isFail()) {
                        complete(
                            Result.fail(
                                when (val ex = it.cause) {
                                    is CancellationException, is Task.Exception -> ex
                                    else -> Task.Exception(
                                        child,
                                        it.cause
                                    )
                                }
                            )
                        )
                    } else
                        complete(ret ?: return@handle)
                }

                except(child::raise)

                child
            }.start(block)
        }

        fun start(block: suspend TaskScope.() -> T): Future<T> {
            synchronized(this) {
                if (status > Task.PENDING)
                    return this

                status = Task.ENQUEUED
            }

            block.startCoroutine(this, this)

            return this
        }

        private class ChildProcess<T>(
            override val name: String,
            parent: ProcessImpl<*>
        ) :
            ProcessImpl<T>(parent.task) {
            override val id: String = "${parent.id}/$name"

        }

        companion object : CoroutineContext.Key<ProcessImpl<*>>
    }

    private class ContextImpl(
        task: Task,
        var worker: Task.Worker? = null
    ) : Task.Context, ProcessImpl<Unit>(task) {
        override val id: String
            get() = "$name#${worker?.id ?: "???"}"

        override val name: String
            get() = task.id

        override fun cancel() {
            thread?.cancel()
            raise(CancellationException())
        }
    }

    private class Worker(
        id: Int
    ) : Thread(threadGroup, "worker-$id") {
        var interupptedAt: Date? = null

        var cancelling: Boolean = false
        var closing: Boolean = false

        init {
            isDaemon = true

            start()
        }

        override fun run() {
            while (true) {
                var task: ProcessImpl<*>? = null

                try {
                    synchronized(this) {
                        interupptedAt = null
                        cancelling = false
                    }

                    if (closing)
                        break

                    val (context, work) = queue.take()
                    task = context[ProcessImpl]!!

                    if (task.isCompleted)
                        continue

                    task.thread = this
                    task.status = Task.RUNNING

                    work.run()
                } catch (ex: InterruptedException) {
                    if (!cancelling && !closing)
                        Scheduler.error("Interuptted while running ${task?.id ?: name}!", ex)
                } catch (ex: Exception) {
                    if (task != null)
                        task.raise(Task.Exception(task, ex))
                    else
                        Scheduler.error("An exception was thrown while executing $name!", ex)

                } finally {
                    if (task == null)
                        continue

                    task.thread = null
                    task.status = Task.SUSPENDED
                }
            }
        }

        @Synchronized
        fun cancel() {
            synchronized(this) {
                interupptedAt = Date()
                cancelling = true

                interrupt()
            }
        }

    }
}