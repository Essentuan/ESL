package net.essentuan.esl.reflections

import net.essentuan.esl.collections.multimap.Multimaps
import net.essentuan.esl.collections.multimap.concurrentValues
import net.essentuan.esl.reflections.Reflections.acquire
import net.essentuan.esl.reflections.extensions.contains
import net.essentuan.esl.reflections.extensions.extends
import net.essentuan.esl.reflections.extensions.isNullable
import net.essentuan.esl.reflections.extensions.javaClass
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.instanceParameter

class Properties : Sequence<KProperty<*>> {
    internal val annotatedWith = Multimaps.concurrentKeys().concurrentValues<Class<*>, KProperty<*>>()
    internal val typeOf = Multimaps.concurrentKeys().concurrentValues<Class<*>, KProperty<*>>()

    internal val all = typeOf[Any::class.java]

    fun <T : Annotation> annotatedWith(cls: Class<T>): Sequence<KProperty<*>> =
        acquire { annotatedWith[cls].asSequence() }

    fun <T : Annotation> annotatedWith(cls: KClass<T>): Sequence<KProperty<*>> =
        annotatedWith(cls.java)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> typeOf(cls: Class<T>): Sequence<KProperty<T?>> =
        acquire { typeOf[cls].asSequence() as Sequence<KProperty<T?>> }

    fun <T : Any> typeOf(cls: KClass<T>): Sequence<KProperty<T?>> = typeOf(cls.java)

    override fun iterator(): Iterator<KProperty<*>> = acquire { all.iterator() }

    companion object {
        fun <T : KProperty<*>, U : Annotation> Sequence<T>.annotatedWith(cls: KClass<U>): Sequence<T> =
            filter { cls in it }

        fun <T : KProperty<*>, U : Annotation> Sequence<T>.annotatedWith(cls: Class<U>): Sequence<T> =
            annotatedWith(cls.kotlin)

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> Sequence<KProperty<*>>.typeOf(cls: KClass<T>): Sequence<KProperty<T?>> =
            filter { it.returnType.javaClass extends cls } as Sequence<KProperty<T?>>

        fun <T : Any> Sequence<KProperty<*>>.typeOf(cls: Class<T>): Sequence<KProperty<T?>> =
            typeOf(cls.kotlin)

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> Sequence<KProperty<T?>>.notNull(): Sequence<KProperty<T>> =
            filter { !it.isNullable } as Sequence<KProperty<T>>

        fun <T : Any> Sequence<KProperty<T?>>.nullable(): Sequence<KProperty<T?>> =
            filter { it.isNullable }

        fun <T : KProperty<*>> Sequence<T>.members(): Sequence<T> =
            filter { it.instanceParameter != null }

        fun <T : KProperty<*>> Sequence<T>.extensions(): Sequence<T> =
            filter { it.extensionReceiverParameter != null }

        fun <T : KProperty<*>> Sequence<T>.static(): Sequence<T> =
            filter { it.instanceParameter == null && it.extensionReceiverParameter == null }
    }
}