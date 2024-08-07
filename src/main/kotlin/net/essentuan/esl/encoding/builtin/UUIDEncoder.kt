package net.essentuan.esl.encoding.builtin

import net.essentuan.esl.encoding.StringBasedEncoder
import net.essentuan.esl.string.extensions.toUUID
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.util.UUID

object UUIDEncoder: StringBasedEncoder<UUID>() {
    override fun encode(obj: UUID, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        obj.toString()

    override fun decode(obj: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): UUID =
        obj.toUUID()
}