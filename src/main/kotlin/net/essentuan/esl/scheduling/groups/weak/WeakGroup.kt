package net.essentuan.esl.scheduling.groups.weak

import com.google.common.collect.MapMaker
import net.essentuan.esl.reflections.extensions.annotatedWith
import net.essentuan.esl.reflections.extensions.get
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.reflections.extensions.visit
import net.essentuan.esl.scheduling.Scheduler
import net.essentuan.esl.scheduling.Task
import net.essentuan.esl.scheduling.annotations.Every
import net.essentuan.esl.scheduling.annotations.Lifetime
import net.essentuan.esl.scheduling.annotations.Workers
import net.essentuan.esl.scheduling.annotations.duration
import net.essentuan.esl.scheduling.id
import net.essentuan.esl.scheduling.impl.GroupImpl
import net.essentuan.esl.scheduling.impl.TaskImpl
import net.essentuan.esl.time.duration.seconds
import java.lang.ref.Cleaner
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredMemberFunctions

@OptIn(ExperimentalStdlibApi::class)
internal class WeakGroup(
    owner: Any
) : GroupImpl("${owner::class.simpleString()}${System.identityHashCode(owner).toHexString()}") {
    private val owner = WeakReference(owner)

    init {
        owner::class.visit()
            .map { it.kotlin }
            .flatMap { it.declaredMemberFunctions }
            .filter { it annotatedWith Every::class }
            .filter { it.parameters.size == 1 && it.parameters[0].kind == KParameter.Kind.INSTANCE }
            .forEach {
                this += WeakTask(it)
            }

        Scheduler += this
        cleaner.register(owner, this::close)
    }

    private inner class WeakTask(
        private val executor: KFunction<*>
    ) : TaskImpl(
        "$name$${executor.id}",
        executor[Every::class]!!.duration(),
        executor[Lifetime::class]?.duration() ?: 10.seconds,
        executor[Workers::class]?.value ?: 1
    ) {
        override suspend fun invoke() {
            executor.callSuspend(owner.get() ?: return)
        }

        override fun close(cancel: Boolean) {
            this@WeakGroup -= this

            
            super.close(cancel)
        }
    }

    companion object : MutableMap<Any, WeakGroup> by MapMaker().weakKeys().makeMap() {
        private val cleaner = Cleaner.create()
    }
}

val Any.tasks: Task.Group
    get() = WeakGroup.getOrPut(this) { WeakGroup(this) }