package net.essentuan.esl.reflections

import javassist.bytecode.ClassFile
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.essentuan.esl.coroutines.await
import net.essentuan.esl.coroutines.blocking
import net.essentuan.esl.coroutines.dispatch
import net.essentuan.esl.coroutines.launch
import net.essentuan.esl.future.api.Completable
import net.essentuan.esl.future.api.Future
import net.essentuan.esl.future.api.except
import net.essentuan.esl.other.lock
import net.essentuan.esl.reflections.extensions.javaClass
import net.essentuan.esl.reflections.extensions.`package`
import net.essentuan.esl.reflections.extensions.visit
import org.reflections.scanners.Scanner
import org.reflections.util.NameHelper
import java.util.Collections
import kotlin.random.Random
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.instanceParameter

typealias Scan = org.reflections.Reflections

private val Names = object : NameHelper {}

object Reflections {
    private var active = mutableSetOf<Int>()
    private var completable: Completable<Unit>? = null
    private var throwable: Throwable? = null
    val types: Types = Types()
    val functions: Functions = Functions()
    val properties: Properties = Properties()

    init {
        register("net.essentuan.esl")
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun register(
        vararg packages: String
    ) {
        if (completable == null) {
            completable = Completable()

            completable!!.exec {
                throwable = null
                completable = null
            }
        }

        val id: Int = Random.nextInt()

        active.lock { add(id) }

        dispatch {
            Scan(
                *packages,
                Scanner {
                    launch {
                        index(it)
                    }

                    listOf()
                }
            )
        } except { ex: Throwable ->
            id.complete(ex)
        } then {
            id.complete()
        }
    }

    internal inline fun <T> acquire(block: () -> T): T {
        completable?.run {
            blocking {
                await()
            }
        }

        return block()
    }

    private fun Int.complete(ex: Throwable? = null) {
        synchronized(active) {
            active.remove(this)

            if (ex != null)
                throwable = throwable ?: ex

            if (active.isNotEmpty()) return

            if (throwable != null)
                completable!!.raise(throwable!!)
            else
                completable!!.complete(Unit)
        }
    }

    private fun index(classFile: ClassFile) {
        val kotlin: KClass<*> = Names.forClass(classFile.name).kotlin

        types.all += kotlin

        kotlin.annotations.forEach { types.annotatedWith[it.annotationClass.java] += kotlin }

        kotlin.visit().forEach { types.subtypes[it] += kotlin }

        fun scan(it: KCallable<*>) {
            when (it) {
                is KFunction<*> -> {
                    functions.all += it

                    it.annotations.forEach { anno ->
                        functions.annotatedWith[anno.annotationClass.java] += it
                    }

                    it.parameters
                        .asSequence()
                        .filter { p -> p != it.instanceParameter }
                        .map { p -> p.type.javaClass }
                        .toList()
                        .apply { functions.withSignature[this] += it }

                    it.returnType
                        .javaClass
                        .visit()
                        .forEach { type -> functions.returns[type] += it }
                }

                is KProperty<*> -> {
                    properties.all += it

                    it.annotations.forEach { anno ->
                        properties.annotatedWith[anno.annotationClass.java]
                    }

                    it.returnType
                        .javaClass
                        .visit()
                        .forEach { type -> properties.typeOf[type] += it }
                }
            }
        }

        kotlin.declaredMembers.forEach { scan(it) }

        if (classFile.name.endsWith("Kt"))
            kotlin.`package`.members.forEach { scan(it) }
    }
}