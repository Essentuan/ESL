@file:Suppress("UNCHECKED_CAST")

package net.essentuan.esl.encoding.providers

import net.essentuan.esl.encoding.Encoder
import net.essentuan.esl.encoding.JsonBasedEncoder
import net.essentuan.esl.encoding.Provider
import net.essentuan.esl.encoding.builtin.AnyEncoder
import net.essentuan.esl.json.json
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.other.lock
import net.essentuan.esl.reflections.extensions.classOf
import net.essentuan.esl.reflections.extensions.extends
import net.essentuan.esl.reflections.extensions.typeArgs
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.util.EnumMap
import java.util.Objects
import kotlin.math.ceil

private const val NULL_KEY = "UkFORE9NU1RSSU5HRk9STlVMTERTQUpEQVNLTERBSlNLTERBUw"

object MapProvider : Provider<MutableMap<Any?, Any?>, AnyJson> {
    override val type: Class<MutableMap<Any?, Any?>>
        get() = MutableMap::class.java as Class<MutableMap<Any?, Any?>>
    private val mystery = MysteryType()

    private val known: MutableMap<Int, KnownType<*, *>> = mutableMapOf()

    private fun hash(key: Type, value: Type, element: AnnotatedElement): Int =
        Objects.hash(key, value, element.annotations.contentHashCode())

    override fun invoke(
        cls: Class<in MutableMap<Any?, Any?>>,
        element: AnnotatedElement,
        vararg typeArgs: Type
    ): Encoder<MutableMap<Any?, Any?>, AnyJson> {
        return if (typeArgs.size != 2)
            mystery
        else
            lock { known.getOrPut(hash(typeArgs[0], typeArgs[1], element)) {
                KnownType<Any, Any>(typeArgs[0], typeArgs[1], element)
            } as Encoder<MutableMap<Any?, Any?>, AnyJson> }
    }

    private class KnownType<K : Any, V: Any>(
        keyType: Type,
        valueType: Type,
        element: AnnotatedElement
    ) : JsonBasedEncoder<MutableMap<K?, V?>>() {
        val keyEncoder: Encoder<K, Any> =
            Encoder(keyType.classOf(), element, *keyType.typeArgs()) as Encoder<K, Any>

        val valueEncoder: Encoder<V, Any> =
            Encoder(valueType.classOf(), element, *valueType.typeArgs()) as Encoder<V, Any>

        override fun encode(
            obj: MutableMap<K?, V?>,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): AnyJson = types(*typeArgs).run {
            obj.encode(
                keyEncoder,
                valueEncoder,
                flags,
                element,
                *typeArgs
            )
        }

        override fun decode(
            obj: AnyJson,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): MutableMap<K?, V?> = types(*typeArgs).run {
            obj.decode(
                keyEncoder,
                this[0].classOf(),
                valueEncoder,
                this[1].classOf(),
                flags,
                element,
                *typeArgs
            )
        }
    }

    private class MysteryType : JsonBasedEncoder<MutableMap<Any?, Any?>>() {
        override fun encode(
            obj: MutableMap<Any?, Any?>,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): AnyJson = types(*typeArgs).run {
            obj.encode(
                this[0].encoder(),
                this[0].encoder(),
                flags,
                element,
                *typeArgs
            )
        }

        override fun decode(
            obj: AnyJson,
            flags: Set<Any>,
            type: Class<*>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): MutableMap<Any?, Any?> = types(*typeArgs).run {
            obj.decode(
                this[0].encoder(),
                this[0].classOf(),
                this[1].encoder(),
                this[1].classOf(),
                flags,
                element,
                *typeArgs
            )
        }
    }
}

private fun types(vararg typeArgs: Type): Array<out Type> {
    return if (typeArgs.size != 2)
        arrayOf(Any::class.java, Any::class.java)
    else
        return typeArgs
}

private fun Type?.encoder(): Encoder<Any, Any> {
    return if (this == null)
        AnyEncoder as Encoder<Any, Any>
    else
        Encoder(classOf()) as Encoder<Any, Any>
}

private fun <K : Enum<K>> enumMap(cls: Class<*>): MutableMap<K, Any?> = EnumMap<K, Any?>(cls as Class<K>)

private fun capacity(numMappings: Int): Int {
    return ceil(numMappings / 0.75).toInt()
}

private fun <K: Any, V: Any> MutableMap<K?, V?>.encode(
    keyEncoder: Encoder<K, Any>,
    valueEncoder: Encoder<V, Any>,
    flags: Set<Any>,
    element: AnnotatedElement,
    vararg typeArgs: Type
): AnyJson = json {
    this@encode.forEach { (k, v) ->
        if (k == null) NULL_KEY else {
            keyEncoder.toString(
                k,
                flags,
                k.javaClass,
                element,
                *typeArgs
            )
        } to if (v == null) null else {
            valueEncoder.encode(
                v,
                flags,
                v.javaClass,
                element,
                *typeArgs
            )
        }
    }
}

private fun <K: Any, V: Any> AnyJson.decode(
    keyEncoder: Encoder<K, Any>,
    keyType: Class<*>,
    valueEncoder: Encoder<V, Any>,
    valueType: Class<*>,
    flags: Set<Any>,
    element: AnnotatedElement,
    vararg typeArgs: Type
): MutableMap<K?, V?> {
    val out = if (keyType extends Enum::class)
        enumMap(keyType) as MutableMap<K?, V?>
        else
            LinkedHashMap(capacity(size))

    entries.forEach {
        val key: K? = if (it.key == NULL_KEY) null else {
            keyEncoder.valueOf(
                it.key,
                flags,
                keyType,
                element,
                *typeArgs
            )
        }

        out[key] = if (it.value == null) null else {
            valueEncoder.decode(
                it.value!!,
                flags,
                valueType,
                element,
                *typeArgs
            )
        }
    }

    return out
}