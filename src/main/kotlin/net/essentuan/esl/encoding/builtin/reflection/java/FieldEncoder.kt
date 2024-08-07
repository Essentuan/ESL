package net.essentuan.esl.encoding.builtin.reflection.java

import net.essentuan.esl.encoding.StringBasedEncoder
import net.essentuan.esl.other.Base64
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.Type

object FieldEncoder : StringBasedEncoder<Field>() {
    override fun encode(obj: Field, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        "${Base64.encode(obj.name)}#${ClassEncoder.encode(
            obj.declaringClass,
            flags,
            type,
            element,
            *typeArgs
        )}"

    override fun decode(obj: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Field? {
        val parts = obj.split('#', limit = 1)

        return ClassEncoder.decode(parts[1], flags, type, element, *typeArgs).getDeclaredField(parts[0])
    }
}