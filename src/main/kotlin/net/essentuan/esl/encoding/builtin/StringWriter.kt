package net.essentuan.esl.encoding.builtin

import net.essentuan.esl.encoding.StringBasedEncoder
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

object StringWriter: StringBasedEncoder<String>() {
    override fun encode(obj: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String = obj

    override fun decode(obj: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String = obj
}