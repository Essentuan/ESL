package net.essentuan.esl.model.field

import net.essentuan.esl.json.type.AnyJson
import kotlin.reflect.KParameter

interface Parameter : Field {
    override val element: KParameter

    fun Array<Any?>.append(data: AnyJson, index: Int, flags: Set<Any>): Int

    companion object {
        fun default(type: Class<*>): Any? =
            if (type.isPrimitive) {
                when (type) {
                    Boolean::class.java -> false
                    Char::class.java -> 0.toChar()
                    Byte::class.java -> 0.toByte()
                    Short::class.java -> 0.toShort()
                    Int::class.java -> 0
                    Float::class.java -> 0f
                    Long::class.java -> 0L
                    Double::class.java -> 0.0
                    String::class.java -> ""
                    Void.TYPE -> throw IllegalStateException("Parameter with void type is illegal")
                    else -> throw UnsupportedOperationException("Unknown primitive: $type")
                }
            } else null
    }
}