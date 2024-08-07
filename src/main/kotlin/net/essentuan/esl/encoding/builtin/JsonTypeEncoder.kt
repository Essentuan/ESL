package net.essentuan.esl.encoding.builtin

import net.essentuan.esl.encoding.JsonBasedEncoder
import net.essentuan.esl.json.type.AnyJson
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
object JsonTypeEncoder : JsonBasedEncoder<AnyJson>() {
    override fun encode(obj: AnyJson, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): AnyJson =
        AnyJson.valueOf(obj, type as Class<AnyJson>)

    override fun decode(obj: AnyJson, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): AnyJson =
        AnyJson.valueOf(obj, type as Class<AnyJson>)
}