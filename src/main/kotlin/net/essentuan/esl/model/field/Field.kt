package net.essentuan.esl.model.field

import net.essentuan.esl.comparing.equals
import net.essentuan.esl.encoding.Encoder
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.json.type.JsonType
import net.essentuan.esl.other.lock
import net.essentuan.esl.other.repr
import net.essentuan.esl.reflections.extensions.classOf
import net.essentuan.esl.reflections.extensions.declaringClass
import net.essentuan.esl.reflections.extensions.get
import net.essentuan.esl.reflections.extensions.typeArgs
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.util.Arrays
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.javaType

private typealias IType = Type

interface Field : AnnotatedElement {
    val key: String?
    val aliases: Array<String>
    val element: KAnnotatedElement

    val type: Field.Type

    override fun <T : Annotation> getAnnotation(cls: Class<T>): T? =
        element[cls]

    override fun getAnnotations(): Array<out Annotation> =
        element.annotations.toTypedArray()

    override fun getDeclaredAnnotations(): Array<out Annotation> =
        getAnnotations()

    fun AnyJson.find(): JsonType.Value? {
        for (key in aliases)
            return this[key] ?: continue

        return null
    }

    class Type(
        val cls: Class<*>,
        val args: Array<IType>,
        val isNullable: Boolean,
        element: AnnotatedElement
    ) {
        @Suppress("UNCHECKED_CAST")
        val encoder: Encoder<Any, Any> = Encoder(cls, element, *args) as Encoder<Any, Any>

        @OptIn(ExperimentalStdlibApi::class)
        constructor(
            type: KType,
            element: AnnotatedElement
        ) : this(
            type.javaType.classOf(),
            type.javaType.typeArgs(),
            type.isMarkedNullable,
            element
        )

        override fun equals(other: Any?): Boolean = equals(other) { it, other ->
            ::cls
            ::isNullable

            Arrays.equals(it.args, other.args)
        }

        override fun hashCode(): Int =
            Arrays.deepHashCode(arrayOf(cls, args, isNullable))

        override fun toString(): String = repr {
            + ::cls
            + ::args
            + ::isNullable
        }
    }

    companion object : Collection<Field> {
        private val members = mutableSetOf<KAnnotatedElement>()
        private val cache = mutableMapOf<Class<*>, MutableMap<KAnnotatedElement, Field>>()

        operator fun get(
            element: KAnnotatedElement,
            owner: KClass<*> = ((element as KProperty<*>).declaringClass)
        ): Field? =
            cache[owner.java]?.get(element)

        operator fun set(
            element: KAnnotatedElement,
            owner: KClass<*> = ((element as KProperty<*>).declaringClass),
            field: Field
        ) {
            members.lock { add(element) }

            cache.lock {
                computeIfAbsent(owner.java) { mutableMapOf<KAnnotatedElement, Field>() }.put(element, field)
            }
        }

        operator fun contains(element: KAnnotatedElement): Boolean =
            element in members

        override fun contains(element: Field): Boolean =
            element.element in this

        override val size: Int
            get() = cache.size

        override fun isEmpty(): Boolean =
            cache.isEmpty()

        override fun iterator(): Iterator<Field> =
            cache.values
                .asSequence()
                .flatMap { it.values }
                .iterator()

        override fun containsAll(elements: Collection<Field>): Boolean {
            for (e in elements)
                if (e !in this)
                    return false

            return true
        }
    }
}