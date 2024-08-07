package net.essentuan.esl.encoding

import com.google.gson.internal.Primitives
import net.essentuan.esl.collections.maps.registry
import net.essentuan.esl.encoding.builtin.AnyEncoder
import net.essentuan.esl.json.Json
import net.essentuan.esl.other.lock
import net.essentuan.esl.other.unsupported
import net.essentuan.esl.reflections.Annotations
import net.essentuan.esl.reflections.Reflections
import net.essentuan.esl.reflections.Types.Companion.objects
import net.essentuan.esl.reflections.extensions.annotatedWith
import net.essentuan.esl.reflections.extensions.instance
import net.essentuan.esl.reflections.extensions.simpleString
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.util.LinkedList

interface Encoder<T : Any, OUT : Any> {
    val type: Class<T>
    val out: Class<OUT>

    fun encode(obj: T, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): OUT?

    fun decode(obj: OUT, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): T?

    fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): T

    fun toString(obj: T, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String

    @Suppress("UNCHECKED_CAST")
    companion object {
        private val DEFAULT_ENCODER = object : Encoder<Any, Any> {
            override fun decode(
                obj: Any,
                flags: Set<Any>,
                type: Class<*>,
                element: AnnotatedElement,
                vararg typeArgs: Type
            ): Any = obj

            override val type: Class<Any>
                get() = Any::class.java
            override val out: Class<Any>
                get() = Any::class.java

            override fun valueOf(
                string: String,
                flags: Set<Any>,
                type: Class<*>,
                element: AnnotatedElement,
                vararg typeArgs: Type
            ): Any = string

            override fun toString(
                obj: Any,
                flags: Set<Any>,
                type: Class<*>,
                element: AnnotatedElement,
                vararg typeArgs: Type
            ): String {
                if (obj is String)
                    return obj

                unsupported()
            }

            override fun encode(
                obj: Any,
                flags: Set<Any>,
                type: Class<*>,
                element: AnnotatedElement,
                vararg typeArgs: Type
            ): Any = obj
        }

        private val providers by lazy {
            val providers = registry<Class<*>, Provider<*, *>> {
                sequence {
                    val queue = LinkedList<Class<*>>()
                    queue.add(it)

                    while (queue.isNotEmpty()) {
                        val cls = queue.pop()

                        if (cls == Any::class.java)
                            continue

                        yield(cls)

                        cls.superclass?.also {
                            if (it != Any::class.java)
                                queue.offerFirst(it)
                        }

                        val interfaces = cls.interfaces

                        if (cls.isInterface)
                            for (i in interfaces.lastIndex downTo 0)
                                queue.offerFirst(interfaces[i])
                        else
                            for (i in interfaces.indices)
                                queue.offer(interfaces[i])
                    }

                    yield(Any::class.java)
                }
            }

            Reflections.types
                .subtypesOf(Provider::class)
                .objects()
                .map { it.instance }
                .filterNotNull()
                .forEach {
                    providers[it.type] = it
                }

            Reflections.types
                .subtypesOf(Encoder::class)
                .objects()
                .map { it.instance }
                .filterNotNull()
                .forEach {
                    providers[it.type] = object : Provider<Any, Any> {
                        override val type: Class<Any>
                            get() = it.type as Class<Any>

                        override fun invoke(
                            cls: Class<in Any>,
                            element: AnnotatedElement,
                            vararg typeArgs: Type
                        ): Encoder<Any, Any> {
                            return it as Encoder<Any, Any>
                        }
                    }
                }

            providers
        }

        operator fun <T : Any> invoke(
            cls: Class<in T>,
            element: AnnotatedElement = Annotations.empty(),
            vararg typeArgs: Type
        ): Encoder<T, *> =
            requireNotNull(
                lock { providers[Primitives.wrap(cls)] as Provider<T, *>? }?.invoke(cls, element, *typeArgs).run {
                    if (this is AnyEncoder && !(element annotatedWith Unsafe::class))
                        DEFAULT_ENCODER as Encoder<T, *>
                    else
                        this
                }
            ) {
                "No encoder exists for ${cls.simpleString()}!"
            }
    }
}

fun Any?.encode(
    element: AnnotatedElement = Annotations.empty(),
    flags: Set<Any> = emptySet(),
    vararg typeArgs: Type = emptyArray()
): Any? {
    return when (this) {
        is Array<*> -> this.map { it.encode(element, flags, *typeArgs) }
        is Iterable<*> -> this.map { it.encode(element, flags, *typeArgs) }
        is Map<*, *> -> {
            this.entries.asSequence()
                .map {
                    Encoder(it.key?.javaClass ?: return@map null to it.value.encode(element, flags, *typeArgs))
                        .toString(it.key!!, flags, it.key!!.javaClass, element, *typeArgs) to it.value.encode(
                        element,
                        flags,
                        *typeArgs
                    )
                }.run {
                    val json = Json()

                    this.forEach {
                        json[it.first!!] = it.second
                    }
                }
        }

        is Json -> this
        else -> Encoder(this?.javaClass ?: return null).encode(
            this,
            flags,
            this.javaClass,
            element,
            *typeArgs
        )
    }
}

@Suppress("UNCHECKED_CAST")
fun Any?.decode(
    flags: Set<Any> = emptySet(),
    type: Class<*>,
    element: AnnotatedElement = Annotations.empty(),
    vararg typeArgs: Type = emptyArray()
): Any? {
    return (Encoder(type) as AbstractEncoder<Any, Any>).decode(this ?: return null, flags, type, element, *typeArgs)
}

inline fun <reified T> Any?.decode(
    flags: Set<Any> = emptySet(),
    element: AnnotatedElement = Annotations.empty(),
    vararg typeArgs: Type = emptyArray()
) = this.decode(flags, T::class.java, element, *typeArgs) as T