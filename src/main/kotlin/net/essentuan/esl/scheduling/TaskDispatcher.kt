package net.essentuan.esl.scheduling

import com.google.common.collect.MapMaker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import net.essentuan.esl.collections.mutableSetFrom
import net.essentuan.esl.time.duration.FormatFlag
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
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
        val task = requireNotNull(context[Task]) { "Only tasks can be submitted to TaskDispatcher!" }

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
                var task: ContextImpl? = null

                try {
                    synchronized(this) {
                        interupptedAt = null
                        cancelling = false
                    }

                    if (closing)
                        break

                    val (context, work) = queue.take()
                    task = context[Task] as ContextImpl

                    if (task.isCompleted)
                        continue

                    synchronized(task) {
                        task.thread = this
                        task.state = Task.State.RUNNING
                    }

                    work.run()
                } catch (ex: InterruptedException) {
                    if (!cancelling && !closing)
                        Scheduler.error("Interuptted while running ${task?.id ?: name}!", ex)
                } catch (ex: Exception) {
                    Scheduler.error("An exception was thrown while running ${task?.id ?: name}!", ex)
                } finally {
                    synchronized(task ?: continue) {
                        task.thread = null
                        task.state = Task.State.SUSPENDED
                    }
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

    private class ContextImpl(
        override val task: Task,
        var worker: Task.Worker? = null,
        var thread: Worker? = null,
        private var completion: ((Result<Unit>) -> Unit)? = null
    ) : Task.Context, Continuation<Unit> {
        val id: String
            get() = "${task.id}#${worker?.id ?: "???"}"

        override val context: CoroutineContext = TaskDispatcher + this

        override var state: Task.State = Task.State.PENDING
            @Synchronized get
            @Synchronized set(value) {
                if (isCompleted)
                    return

                field = value
            }

        override val isCompleted: Boolean
            @Synchronized get() = state == Task.State.COMPLETED || state == Task.State.CANCELLED

        override fun start(block: suspend () -> Unit, completion: (Result<Unit>) -> Unit) {
            synchronized(this) {
                require(this.completion == null) { "This context has already been assigned a task!" }

                this.completion = completion
            }

            block.startCoroutine(this)
        }

        override fun resumeWith(result: Result<Unit>) {
            synchronized(this) {
                if (isCompleted)
                    return

                state = if (result.exceptionOrNull() is CancellationException)
                    Task.State.CANCELLED
                else
                    Task.State.COMPLETED
            }

            completion?.invoke(result)
        }

        override fun cancel() {
            thread?.cancel()
            resumeWith(Result.failure(CancellationException()))
        }
    }
}