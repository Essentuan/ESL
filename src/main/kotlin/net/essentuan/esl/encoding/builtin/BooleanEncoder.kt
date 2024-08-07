package net.essentuan.esl.encoding.builtin

import net.essentuan.esl.encoding.AbstractEncoder
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

object BooleanEncoder : AbstractEncoder<Boolean, Boolean>() {
    override fun encode(obj: Boolean, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Boolean = obj

    override fun decode(obj: Boolean, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Boolean = obj

    override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Boolean = string.toBoolean()

    override fun toString(obj: Boolean, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String = obj.toString()
}