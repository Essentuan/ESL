package net.essentuan.esl.encoding.builtin

import net.essentuan.esl.encoding.AbstractEncoder
import net.essentuan.esl.encoding.Encoder
import net.essentuan.esl.json.Json
import net.essentuan.esl.json.json
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.other.Base64
import net.essentuan.esl.reflections.extensions.typeArgs
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

object AnyEncoder : AbstractEncoder<Any, AnyJson>() {
    override fun encode(obj: Any, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): AnyJson {
        val writer = Encoder(obj.javaClass)

        return json {
            "value" to if (writer is AnyEncoder) obj else writer.encode(obj, flags, obj.javaClass, element, *typeArgs)
            "type" to Base64.encode(obj.javaClass.name)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun decode(obj: AnyJson, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Any? {
        require(obj.isNotEmpty())

        val cls = Class.forName(Base64.decode(obj.getString("type")!!))
        val writer = Encoder(cls) as Encoder<*, *>

        return if (writer is AnyEncoder)
            obj["value"]!!.raw
        else
            (writer as Encoder<Any, Any>).decode(obj["value"]!!.raw!!, flags, cls, element, *typeArgs[0].typeArgs())
    }

    override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Any =
        decode(Json(string), flags, type, element, *typeArgs)!!

    override fun toString(obj: Any, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        Json(encode(obj, flags, type, element, *typeArgs)).asString()
}