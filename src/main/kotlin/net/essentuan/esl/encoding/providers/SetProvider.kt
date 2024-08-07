@file:Suppress("UNCHECKED_CAST")

package net.essentuan.esl.encoding.providers

import net.essentuan.esl.encoding.AbstractEncoder
import net.essentuan.esl.encoding.Encoder
import net.essentuan.esl.encoding.Provider
import net.essentuan.esl.encoding.builtin.AnyEncoder
import net.essentuan.esl.other.lock
import net.essentuan.esl.other.unsupported
import net.essentuan.esl.reflections.extensions.classOf
import net.essentuan.esl.reflections.extensions.extends
import net.essentuan.esl.reflections.extensions.typeArgs
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.util.EnumSet
import java.util.Objects
import kotlin.math.ceil

object SetProvider : Provider<MutableSet<Any?>, List<Any?>> {
    override val type: Class<MutableSet<Any?>>
        get() = MutableSet::class.java as Class<MutableSet<Any?>>
    private val mystery = MysteryType()

    private val known: MutableMap<Int, KnownType<*>> = mutableMapOf()

    private fun hash(type: Type, element: AnnotatedElement): Int =
        Objects.hash(type, element.annotations.contentHashCode())

    override fun invoke(
        cls: Class<in MutableSet<Any?>>,
        element: AnnotatedElement,
        vararg typeArgs: Type
    ): Encoder<MutableSet<Any?>, List<Any?>> {
        return if (typeArgs.size != 1)
            mystery
        else
            lock {known.getOrPut(hash(typeArgs[0], element)) {
                KnownType<Any>(typeArgs[0], element)
            } as Encoder<MutableSet<Any?>, List<Any?>> }
    }

    private class KnownType<T : Any>(type: Type, element: AnnotatedElement) :
        AbstractEncoder<MutableSet<T?>, List<Any?>>() {
        val encoder: Encoder<T, Any> = Encoder(type.classOf(), element, *type.typeArgs()) as Encoder<T, Any>

        override fun encode(
            obj: MutableSet<T?>,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): List<Any?> {
            return typeOf(*typeArgs).run {
                obj.encode(
                    encoder,
                    flags,
                    element,
                    *this.typeArgs()
                )
            }
        }

        override fun decode(
            obj: List<Any?>,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): MutableSet<T?> {
            return typeOf(*typeArgs).run {
                obj.decode(
                    encoder,
                    flags,
                    this.classOf(),
                    element,
                    *this.typeArgs()
                )
            }
        }

        override fun valueOf(
            string: String,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): MutableSet<T?> = unsupported()

        override fun toString(
            obj: MutableSet<T?>,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): String = unsupported()
    }

    private class MysteryType : AbstractEncoder<MutableSet<Any?>, List<Any?>>() {
        override fun encode(
            obj: MutableSet<Any?>,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): List<Any?> {
            return typeOf(*typeArgs).run {
                obj.encode(
                    encoder(*typeArgs),
                    flags,
                    element,
                    *this.typeArgs()
                )
            }
        }

        override fun decode(
            obj: List<Any?>,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): MutableSet<Any?> {
            return typeOf(*typeArgs).run {
                obj.decode(
                    encoder(*typeArgs),
                    flags,
                    this.classOf(),
                    element,
                    *this.typeArgs()
                )
            }
        }

        override fun valueOf(
            string: String,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): MutableSet<Any?> = unsupported()

        override fun toString(
            obj: MutableSet<Any?>,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): String = unsupported()
    }
}

private fun typeOf(vararg typeArgs: Type): Type {
    return if (typeArgs.size != 1)
        Any::class.java
    else
        typeArgs[0]
}

private fun encoder(
    vararg typeArgs: Type
): Encoder<Any, Any> {
    return if (typeArgs.size != 1)
        AnyEncoder as Encoder<Any, Any>
    else
        Encoder(typeArgs[0].classOf()) as Encoder<Any, Any>
}

private fun <K : Enum<K>> enumSet(cls: Class<*>): MutableSet<K> = EnumSet.noneOf(cls as Class<K>)

private fun capacity(numMappings: Int): Int {
    return ceil(numMappings / 0.75).toInt()
}

private fun <T : Any> List<Any?>.decode(
    encoder: Encoder<T, Any>,
    flags: Set<Any>,
    type: Class<*>,
    element: AnnotatedElement,
    vararg typeArgs: Type,
): MutableSet<T?> {
    val out: MutableSet<T?> = if (type extends Enum::class)
        enumSet(type) as MutableSet<T?>
    else
        LinkedHashSet(capacity(size))

    forEach {
        out += if (it == null) null else {
            encoder.decode(
                it,
                flags,
                type,
                element,
                *typeArgs
            )
        }
    }

    return out
}

private fun <T : Any> MutableSet<T?>.encode(
    encoder: Encoder<T, Any>,
    flags: Set<Any>,
    element: AnnotatedElement,
    vararg typeArgs: Type,
): List<Any?> {
    val out = ArrayList<Any?>(size)

    forEach {
        out += if (it == null) null else {
            encoder.encode(
                it,
                flags,
                it.javaClass,
                element,
                *typeArgs
            )
        }
    }

    return out
}