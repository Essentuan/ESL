package net.essentuan.esl.scheduling

import net.essentuan.esl.future.api.Completable
import net.essentuan.esl.future.api.Future
import net.essentuan.esl.other.lock
import net.essentuan.esl.other.unsupported
import net.essentuan.esl.reflections.Functions.Companion.static
import net.essentuan.esl.reflections.Reflections
import net.essentuan.esl.reflections.Types.Companion.objects
import net.essentuan.esl.reflections.extensions.annotatedWith
import net.essentuan.esl.reflections.extensions.extends
import net.essentuan.esl.reflections.extensions.instance
import net.essentuan.esl.reflections.extensions.isStatic
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.reflections.extensions.visit
import net.essentuan.esl.scheduling.annotations.Every
import net.essentuan.esl.scheduling.annotations.isAutoLoaded
import net.essentuan.esl.scheduling.group.AbstractGroup
import net.essentuan.esl.time.duration.Duration
import java.lang.ref.WeakReference
import java.util.Date
import kotlin.reflect.jvm.javaMethod

object Static : AbstractGroup() {
    override val name: String
        get() = "Static"

    private var ready = false

    init {
        Scheduler.groups.lock { this[this@Static] = this@Static }
    }

    internal fun load() {
        if (ready)
            return

        Reflections
            .functions
            .annotatedWith(Every::class)
            .static()
            .map { it.javaMethod }
            .filterNotNull()
            .forEach { Task(it, null) }

        ready = true
    }

    override fun resume() {
        load()

        Ref.load()

        super.resume()
    }

    override fun close() = unsupported()

    class Scheduled(
        uid: String,
        rate: Duration,
        lifetime: Duration,
        action: suspend () -> Unit,
        private val completable: Completable<Unit> = Completable()
    ) : Task(uid, rate, lifetime, 1, action), Future<Unit> by completable {
        init {
            lastExecuted = Date()
        }

        override fun finish(worker: Worker) {
            completable.complete(Unit)
            close(false)
        }
    }
}

object Ref {
    private var ready: Boolean = false

    internal fun load() {
        if (!ready)
            Reflections.types
                .objects()
                .filterNot { it extends Static::class }
                .forEach {
                    val tasks = it.instance?.tasks ?: return@forEach

                    if (tasks.isNotEmpty() && it.isAutoLoaded)
                        tasks.resume()
                }

        ready = true
    }

    internal class Instance(obj: Any) : AbstractGroup() {
        private val ref = WeakReference(obj)
        override val name: String = obj.javaClass.simpleString()

        init {
            obj.javaClass.visit()
                .flatMap { it.declaredMethods.asIterable() }
                .filter { !it.isStatic() && it annotatedWith Every::class }
                .forEach { Task(it, ref) }
        }

        override fun close() {
            Scheduler.remove(ref.get() ?: return)
        }
    }
}