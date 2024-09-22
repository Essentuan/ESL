package net.essentuan.esl.scheduling

import com.google.common.collect.MapMaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import net.essentuan.esl.delegates.final
import net.essentuan.esl.iteration.extensions.iterate
import net.essentuan.esl.other.Output
import net.essentuan.esl.other.lock
import net.essentuan.esl.reflections.extensions.isReady
import net.essentuan.esl.scheduling.annotations.Every
import net.essentuan.esl.scheduling.api.Task
import net.essentuan.esl.time.duration.seconds
import net.essentuan.esl.time.extensions.timeSince
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

object Scheduler : Iterable<Task.Group>, Output.Group() {
    internal val groups = MapMaker().weakKeys().makeMap<Any, Task.Group>()

    var DISPATCHER: CoroutineScope by final {
        CoroutineScope(Executors.newScheduledThreadPool(capacity).asCoroutineDispatcher())
    }

    private val executor: Thread by lazy {
        thread(name = "Scheduler") {
            val workers = AtomicInteger()
            var lastCompletion = Date()
            var lastRun: Date

            val thread = Thread.currentThread()

            while (!thread.isInterrupted) {
                lastRun = Date()

                synchronized(groups) {
                    try {
                        for (group in groups.lock { values.toList() })
                            for (task in group) {
                                task.cull()

                                if (task.ready())
                                    task.spin({ workers.incrementAndGet() }) {
                                        workers.decrementAndGet()

                                        lastCompletion = Date()
                                    }
                            }
                    } catch (t: Throwable) {
                        if (t !is InterruptedException)
                            fatal("Error in Scheduler", t)
                    }

                    if (lastCompletion.timeSince() - lastRun.timeSince() > 15.seconds && workers.get() >= capacity) {
                        fatal("Scheduler has run out of workers! Currently occupied by:")

                        for (group in groups.values)
                            for (task in group)
                                if (task.isNotEmpty())
                                    task.lock { iterate { fatal(it.toString()) } }

                        finalizers.forEach { it() }
                    }
                }

                Thread.sleep(5)
            }
        }
    }

    var capacity: Int = 10
        set(value) {
            check(!::DISPATCHER.isReady) { "The shared pool has already been created!" }

            field = value
        }


    private val finalizers = mutableListOf<() -> Unit>(this::shutdown)

    fun start() {
        DISPATCHER
        executor

        Static.resume()
    }

    fun shutdown() {
        executor.interrupt()
        (DISPATCHER as? AutoCloseable)?.close()
    }

    infix fun finally(func: () -> Unit) {
        finalizers += func
    }

    fun register(owner: Any, group: Task.Group) {
        groups.lock { computeIfAbsent(owner) { group } }
    }

    fun remove(owner: Any) {
        if (owner is Static)
            return

        groups.lock { remove(owner) }
    }

    override fun iterator(): Iterator<Task.Group> {
        return groups.lock { values.toList() }.iterator()
    }
}

val Any.tasks: Task.Group
    get() {
        return Scheduler.groups.lock {
            computeIfAbsent(this@tasks) {
                val group = Ref.Instance(it)

                if (group.isEmpty()) null else group
            }
        } ?: return EmptyGroup(this.javaClass.simpleName)
    }

@Every(ms = 10.0)
fun alive() = Unit
