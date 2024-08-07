package net.essentuan.esl.encoding.builtin.reflection.java

import net.essentuan.esl.encoding.StringBasedEncoder
import net.essentuan.esl.other.Base64
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

object ClassEncoder: StringBasedEncoder<Class<*>>() {
    override fun encode(obj: Class<*>, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        Base64.encode(obj.name)

    override fun decode(obj: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Class<*> = Class.forName(
        Base64.decode(obj))
}