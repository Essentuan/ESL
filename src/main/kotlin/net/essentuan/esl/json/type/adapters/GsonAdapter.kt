package net.essentuan.esl.json.type.adapters

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.essentuan.esl.iteration.extensions.iterate
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.json.type.JsonType
import net.essentuan.esl.json.type.jsonType
import net.essentuan.esl.other.unsupported
import net.essentuan.esl.reflections.extensions.simpleString
import kotlin.reflect.KClass

abstract class GsonAdapter<To : AnyJson>(val to: Class<To>) : JsonType.Adapter<JsonElement, To> {
    constructor(to: KClass<To>) : this(to.java)

    override fun from(): Class<JsonElement> = JsonElement::class.java
    override fun to(): Class<To> = to

    override fun convert(obj: JsonElement): To {
        return when (obj) {
            is JsonObject -> get(obj)
            is JsonArray -> jsonType(this::empty) {
                "array" to get(obj)
            }

            else -> unsupported("Cannot convert ${obj.javaClass.simpleString()} to ${to().simpleString()}!")
        }
    }

    private fun get(element: JsonElement?): Any? {
        return when (element) {
            is JsonObject -> get(element)
            is JsonPrimitive -> get(element)
            is JsonArray -> get(element)
            else -> null
        }
    }

    private fun get(obj: JsonObject): To =
        empty().apply { obj.entrySet() iterate { this@apply[it.key] = get(it.value) } }

    private fun get(array: JsonArray): List<*> = ArrayList<Any?>().apply {
        array iterate {
            this@apply += get(it)
        }
    }

    private fun get(primitive: JsonPrimitive): Any? {
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> primitive.asNumber
            primitive.isString -> primitive.asString
            else -> null
        }
    }

    abstract fun empty(): To
}