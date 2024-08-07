package net.essentuan.esl.reflections

import net.essentuan.esl.collections.multimap.Multimaps
import net.essentuan.esl.collections.multimap.hashSetValues
import net.essentuan.esl.reflections.Reflections.acquire
import net.essentuan.esl.reflections.extensions.contains
import net.essentuan.esl.reflections.extensions.extends
import net.essentuan.esl.reflections.extensions.isNullable
import net.essentuan.esl.reflections.extensions.isStatic
import net.essentuan.esl.reflections.extensions.javaClass
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmErasure

class Functions internal constructor() : Sequence<KFunction<*>> {
    internal val annotatedWith = Multimaps.hashKeys().hashSetValues<Class<*>, KFunction<*>>()
    internal val withSignature = Multimaps.hashKeys().hashSetValues<List<Class<*>>, KFunction<*>>()
    internal val returns = Multimaps.hashKeys().hashSetValues<Class<*>, KFunction<*>>()

    internal val all = mutableSetOf<KFunction<*>>()

    fun <U : Annotation> annotatedWith(cls: Class<U>): Sequence<KFunction<*>> =
        acquire { annotatedWith[cls].asSequence() }

    fun <U : Annotation> annotatedWith(cls: KClass<U>): Sequence<KFunction<*>> = annotatedWith(cls.java)

    fun withSignature(vararg args: Class<*>): Sequence<KFunction<*>> =
        acquire { withSignature[args.asList()].asSequence() }

    fun withSignature(vararg args: KClass<*>): Sequence<KFunction<*>> =
        withSignature(*Array<Class<*>>(args.size) { args[it].java })

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> returns(cls: Class<T>): Sequence<KFunction<T?>> =
        acquire { returns[cls].asSequence() as Sequence<KFunction<T?>> }

    fun <T : Any> returns(cls: KClass<T>): Sequence<KFunction<T?>> =
        returns(cls.java)

    override fun iterator(): Iterator<KFunction<*>> = acquire {all.iterator() }

    companion object {
        fun <T : KFunction<*>, U : Annotation> Sequence<T>.annotatedWith(cls: KClass<U>): Sequence<T> =
            filter { cls in it }

        fun <T : KFunction<*>, U : Annotation> Sequence<T>.annotatedWith(cls: Class<U>): Sequence<T> =
            annotatedWith(cls.kotlin)

        fun <T : KFunction<*>> Sequence<T>.withSignature(vararg args: KClass<*>): Sequence<T> =
            filter {
                it.parameters.asSequence()
                    .filter { p -> p != it.instanceParameter }
                    .forEachIndexed { i, p ->
                        if (i >= args.size || p.type.jvmErasure != args[i])
                            return@filter true
                    }

                true
            }

        fun <T : KFunction<*>> Sequence<T>.withSignature(vararg args: Class<*>): Sequence<T> =
            withSignature(*Array<KClass<*>>(args.size) { args[it].kotlin })

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> Sequence<KFunction<*>>.returns(cls: KClass<T>): Sequence<KFunction<T?>> =
            filter { it.returnType.javaClass extends cls } as Sequence<KFunction<T?>>

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> Sequence<KFunction<*>>.returns(cls: Class<T>): Sequence<KFunction<T?>> =
            filter { it.returnType.javaClass extends cls } as Sequence<KFunction<T?>>

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> Sequence<KFunction<T?>>.notNull(): Sequence<KFunction<T>> =
            filter { !it.isNullable } as Sequence<KFunction<T>>

        fun <T : Any> Sequence<KFunction<T?>>.nullable(): Sequence<KFunction<T?>> =
            filter { it.isNullable }

        fun <T : KFunction<*>> Sequence<T>.members(): Sequence<T> =
            filter { it.instanceParameter != null && it.javaConstructor == null }

        fun <T : KFunction<*>> Sequence<T>.extensions(): Sequence<T> =
            filter { it.extensionReceiverParameter != null && it.javaConstructor == null }

        fun <T : KFunction<*>> Sequence<T>.static(): Sequence<T> =
            filter { it.javaMethod?.isStatic() == true }

        fun <T : KFunction<*>> Sequence<T>.constructors(): Sequence<T> =
            filter { it.javaConstructor != null }
    }
}