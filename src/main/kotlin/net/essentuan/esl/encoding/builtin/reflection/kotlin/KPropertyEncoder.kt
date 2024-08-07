package net.essentuan.esl.encoding.builtin.reflection.kotlin

import net.essentuan.esl.encoding.StringBasedEncoder
import net.essentuan.esl.encoding.builtin.reflection.java.FieldEncoder
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.kotlinProperty

object KPropertyEncoder : StringBasedEncoder<KProperty<*>>() {
    override fun encode(obj: KProperty<*>, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String? {
        return FieldEncoder.encode(obj.javaField ?: return null, flags, type, element, *typeArgs)
    }

    override fun decode(obj: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): KProperty<*>? =
        FieldEncoder.decode(obj, flags, type, element, *typeArgs)?.kotlinProperty

}