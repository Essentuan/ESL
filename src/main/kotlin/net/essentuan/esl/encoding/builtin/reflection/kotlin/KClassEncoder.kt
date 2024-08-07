package net.essentuan.esl.encoding.builtin.reflection.kotlin

import net.essentuan.esl.encoding.StringBasedEncoder
import net.essentuan.esl.encoding.builtin.reflection.java.ClassEncoder
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import kotlin.reflect.KClass

object KClassEncoder : StringBasedEncoder<KClass<*>>() {
    override fun encode(obj: KClass<*>, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        ClassEncoder.encode(obj.java, flags, type, element, *typeArgs)

    override fun decode(obj: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): KClass<*> =
        ClassEncoder.decode(obj, flags, type, element, *typeArgs).kotlin
}