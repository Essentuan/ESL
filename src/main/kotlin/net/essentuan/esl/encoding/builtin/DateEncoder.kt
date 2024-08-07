package net.essentuan.esl.encoding.builtin

import net.essentuan.esl.encoding.AbstractEncoder
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.time.extensions.toDate
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.util.Date

object DateEncoder : AbstractEncoder<Date, Any>() {
    override fun encode(obj: Date, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Number =
        obj.time

    override fun decode(obj: Any, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Date = when (obj) {
        is Number -> Date(obj.toLong())
        is Date -> obj
        is String -> obj.toDate()
        else -> throw IllegalArgumentException("${obj::class.simpleString()} cannot be cast to Date!")
    }

    override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Date =
        Date(string.toLong())

    override fun toString(obj: Date, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        obj.time.toString()
}