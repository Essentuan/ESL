package net.essentuan.esl.scheduling.group

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.essentuan.esl.delegates.lateinit
import net.essentuan.esl.iteration.extensions.mutable.iterate
import net.essentuan.esl.other.repr
import net.essentuan.esl.reflections.extensions.get
import net.essentuan.esl.reflections.extensions.tags
import net.essentuan.esl.scheduling.Scheduler
import net.essentuan.esl.scheduling.annotations.Every
import net.essentuan.esl.scheduling.annotations.Lifetime
import net.essentuan.esl.scheduling.annotations.Workers
import net.essentuan.esl.scheduling.annotations.duration
import net.essentuan.esl.scheduling.api.Task
import net.essentuan.esl.scheduling.api.id
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.seconds
import net.essentuan.esl.time.extensions.timeSince
import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.Collections
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

internal typealias ITask = Task
internal typealias IWorker = Task.Worker

abstract class AbstractGroup : Task.Group {
    protected var executor: CoroutineScope by lateinit { Scheduler.DISPATCHER }
    protected val tasks: MutableMap<String, ITask> = Collections.synchronizedMap(mutableMapOf())

    override fun get(uid: String): ITask? = tasks[uid]

    override fun get(method: Method): ITask? = tasks[method.id()]

    override fun get(func: KFunction<*>): ITask? {
        return tasks[func.javaMethod?.id() ?: return null]
    }

    override fun contains(uid: String): Boolean = get(uid) != null

    override fun contains(method: Method): Boolean = get(method) != null

    override fun contains(func: KFunction<*>): Boolean = get(func) != null

    override fun contains(element: ITask): Boolean = contains(element.id)

    override val size: Int
        get() = tasks.size

    override fun containsAll(elements: Collection<ITask>): Boolean {
        for (task in elements)
            if (task !in this)
                return false

        return true
    }

    override fun isEmpty(): Boolean = tasks.isEmpty()

    override fun iterator(): Iterator<ITask> = tasks.values.iterator()

    override fun resume() {
        this.forEach { it.resume() }
    }

    override fun suspend(cancel: Boolean) {
        this.forEach { it.suspend(cancel) }
    }

    open inner class Task(
        final override val id: String,
        final override val rate: Duration,
        lifetime: Duration,
        final override val capacity: Int,
        private val action: suspend () -> Unit,
        private val workers: MutableCollection<IWorker> = mutableSetOf(),
    ) : ITask, Collection<IWorker> by workers {
        override var lifetime: Duration = lifetime
            protected set

        final override var suspended: Boolean = true
            private set

        protected lateinit var lastExecuted: Date

        init {
            require(id !in this@AbstractGroup) {
                "$id is already in ${this@AbstractGroup}!"
            }

            this@AbstractGroup.tasks[id] = this
        }

        constructor(method: Method, ref: WeakReference<Any>?) : this(
            method.id(),
            method.tags[Every::class]!!.duration(),
            method.tags[Lifetime::class]?.duration() ?: 10.seconds,
            method.tags[Workers::class]?.value ?: 1,
            method.invoker(ref)
        )

        override fun cull() {
            synchronized(this) {
                workers iterate {
                    if (it.marked()) {
                        Scheduler.warn("$it has exceeded its max age! (${it.age.print()})")

                        it.cancel()
                        remove()
                    } else if (it.isComplete)
                        remove()
                }
            }
        }

        override fun ready(): Boolean {
            return !suspended && size < capacity && (!::lastExecuted.isInitialized || lastExecuted.timeSince() >= rate)
        }

        protected open fun start(worker: Worker) = Unit

        protected open fun finish(worker: Worker) = Unit

        override fun spin(starter: Runnable, finisher: Runnable) {
            check(ready())

            lastExecuted = Date()

            Worker(starter, finisher, size)
        }

        private fun cancel() {
            synchronized(this) {
                workers iterate {
                    it.cancel()
                    remove()
                }
            }
        }

        override fun suspend(cancel: Boolean) {
            suspended = true

            if (cancel) cancel()
        }

        override fun resume() {
            suspended = false
        }


        override fun close(cancel: Boolean) {
            tasks.remove(id)

            if (cancel) cancel()
        }

        override fun toString(): String = repr {
            prefix(Task::class)

            +Task::id
            +Task::rate
            +Task::lifetime
            +Task::capacity
            +Task::suspended

            "workers" to size
        }

        inner class Worker(
            val starter: Runnable,
            val finisher: Runnable,
            override val id: Int
        ) : IWorker {
            override val createdAt: Date = Date()
            override val age: Duration
                get() = createdAt.timeSince()

            val job: Job = executor.launch {
                synchronized(this@Task) { workers.add(this@Worker) }

                start(this@Worker)
                starter.run()

                try {
                    action()
                } catch (t: Throwable) {
                    if (t !is CancellationException)
                        Scheduler.error("Fail to execute $this! Caused by:", t)
                }

                synchronized(this@Task) {
                    workers.remove(this@Worker)
                }

                finish(this@Worker)
                finisher.run()
            }

            override val isComplete: Boolean
                get() = job.isCompleted

            init {
                synchronized(this@Task) { workers.add(this) }
            }

            override fun marked(): Boolean = age > lifetime

            override fun cancel() = job.cancel()

            override fun toString(): String = "$name[Task(id=${this@Task.id}).Worker(id=$id, created=$createdAt)]"
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun Method.invoker(ref: WeakReference<Any>?): suspend () -> Unit {
    isAccessible = true

    val func = kotlinFunction as KFunction<Unit>?

    return func?.run {
        func.isAccessible = true

        suspend func@{
            if (ref == null) func.callSuspend() else func.callSuspend(ref.get() ?: return@func)
        }
    } ?: method@{
        if (ref == null) invoke(null) else invoke(ref.get() ?: return@method)
    }
}