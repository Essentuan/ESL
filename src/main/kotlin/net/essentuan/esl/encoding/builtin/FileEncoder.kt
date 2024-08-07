package net.essentuan.esl.encoding.builtin

import net.essentuan.esl.encoding.StringBasedEncoder
import java.io.File
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

object FileEncoder : StringBasedEncoder<File>() {
    override fun encode(obj: File, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        obj.absolutePath

    override fun decode(obj: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): File =
        File(obj)
}