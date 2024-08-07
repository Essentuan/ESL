package net.essentuan.esl.encoding.builtin

import net.essentuan.esl.encoding.StringBasedEncoder
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.net.URI
import java.net.URL

object URLEncoder : StringBasedEncoder<URL>() {
    override fun encode(obj: URL, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        obj.toExternalForm()

    override fun decode(obj: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): URL =
        URI(obj).toURL()
}