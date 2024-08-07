package net.essentuan.esl.encoding

import net.essentuan.esl.json.Json
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.json.type.JsonType
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

abstract class JsonBasedEncoder<T: Any> : AbstractEncoder<T, AnyJson> {
    constructor(cls: Class<T>) : super(cls, JsonType::class.java)

    constructor() : super()

    override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): T =
        decode(Json(string), flags, type, element, *typeArgs)!!

    override fun toString(obj: T, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        Json(encode(obj, flags, type, element, *typeArgs)!!).asString()
}