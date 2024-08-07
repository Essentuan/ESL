@file:Suppress("UNCHECKED_CAST")

package net.essentuan.esl.encoding.providers

import net.essentuan.esl.encoding.AbstractEncoder
import net.essentuan.esl.encoding.Encoder
import net.essentuan.esl.encoding.Provider
import net.essentuan.esl.encoding.builtin.AnyEncoder
import net.essentuan.esl.other.lock
import net.essentuan.esl.other.unsupported
import net.essentuan.esl.reflections.extensions.classOf
import net.essentuan.esl.reflections.extensions.typeArgs
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.util.Objects

object ListProvider : Provider<MutableList<Any?>, List<Any?>> {
    override val type: Class<MutableList<Any?>>
        get() = MutableList::class.java as Class<MutableList<Any?>>
    private val mystery = MysteryType()

    private val known: MutableMap<Int, KnownType<*>> = mutableMapOf()

    private fun hash(type: Type, element: AnnotatedElement): Int =
        Objects.hash(type, element.annotations.contentHashCode())

    override fun invoke(
        cls: Class<in MutableList<Any?>>,
        element: AnnotatedElement,
        vararg typeArgs: Type
    ): Encoder<MutableList<Any?>, List<Any?>> {
        return if (typeArgs.size != 1)
            mystery
        else
            lock { known.getOrPut(hash(typeArgs[0], element)) {
                KnownType<Any>(typeArgs[0], element)
            } as Encoder<MutableList<Any?>, List<Any?>> }
    }

    private class KnownType<T : Any>(type: Type, element: AnnotatedElement) :
        AbstractEncoder<MutableList<T?>, List<Any?>>() {
        val encoder: Encoder<T, Any> = Encoder(type.classOf(), element, *type.typeArgs()) as Encoder<T, Any>

        override fun encode(
            obj: MutableList<T?>,
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
        ): MutableList<T?> {
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
        ): MutableList<T?> = unsupported()

        override fun toString(
            obj: MutableList<T?>,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): String = unsupported()
    }

    private class MysteryType : AbstractEncoder<MutableList<Any?>, List<Any?>>() {
        override fun encode(
            obj: MutableList<Any?>,
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
        ): MutableList<Any?> {
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
        ): MutableList<Any?> = unsupported()

        override fun toString(
            obj: MutableList<Any?>,
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

private fun <T : Any> List<Any?>.decode(
    encoder: Encoder<T, Any>,
    flags: Set<Any>,
    type: Class<*>,
    element: AnnotatedElement,
    vararg typeArgs: Type,
): MutableList<T?> {
    val out: MutableList<T?> = ArrayList(size)

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

private fun <T : Any> MutableList<T?>.encode(
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