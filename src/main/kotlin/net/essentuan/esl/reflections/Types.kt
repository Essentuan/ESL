package net.essentuan.esl.reflections

import net.essentuan.esl.collections.multimap.Multimaps
import net.essentuan.esl.collections.multimap.hashSetValues
import net.essentuan.esl.reflections.Reflections.acquire
import net.essentuan.esl.reflections.extensions.contains
import net.essentuan.esl.reflections.extensions.extends
import net.essentuan.esl.reflections.extensions.isObject
import kotlin.reflect.KClass

class Types internal constructor() : Sequence<KClass<*>> {
    internal val subtypes = Multimaps.hashKeys().hashSetValues<Class<*>, KClass<*>>()
    internal val annotatedWith = Multimaps.hashKeys().hashSetValues<Class<*>, KClass<*>>()

    internal val all = subtypes.get(Any::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> subtypesOf(cls: Class<T>): Sequence<KClass<T>> = acquire {
        subtypes[cls].asSequence() as Sequence<KClass<T>>
    }

    fun <T : Any> subtypesOf(cls: KClass<T>): Sequence<KClass<T>> = subtypesOf(cls.java)

    fun <U : Annotation> annotatedWith(cls: Class<U>): Sequence<KClass<*>> = acquire {
        annotatedWith[cls].asSequence()
    }

    fun <U : Annotation> annotatedWith(cls: KClass<U>): Sequence<KClass<*>> = annotatedWith(cls.java)

    override fun iterator(): Iterator<KClass<*>> = acquire { all.iterator() }

    companion object {
        fun <T : KClass<*>> Sequence<T>.abstractTypes(): Sequence<T> = filter { it.isAbstract }

        fun <T : KClass<*>> Sequence<T>.concreteTypes(): Sequence<T> = filter { !it.isAbstract }

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> Sequence<KClass<*>>.subtypesOf(cls: KClass<T>): Sequence<KClass<T>> =
            filter { it extends cls } as Sequence<KClass<T>>

        fun <T : Any> Sequence<KClass<*>>.subtypesOf(cls: Class<T>): Sequence<KClass<T>> = subtypesOf(cls.kotlin)

        fun <T : KClass<*>, U : Annotation> Sequence<T>.annotatedWith(cls: KClass<U>): Sequence<T> =
            filter { cls in it }

        fun <T : KClass<*>, U : Annotation> Sequence<T>.annotatedWith(cls: Class<U>): Sequence<T> =
            annotatedWith(cls.kotlin)

        fun <T : KClass<*>> Sequence<T>.objects(): Sequence<T> = filter { it.isObject }
    }
}