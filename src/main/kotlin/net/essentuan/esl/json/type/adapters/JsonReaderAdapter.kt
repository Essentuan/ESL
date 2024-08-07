package net.essentuan.esl.json.type.adapters

import com.google.gson.internal.LazilyParsedNumber
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import net.essentuan.esl.json.type.Adapters
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.json.type.GsonTypes
import net.essentuan.esl.json.type.JsonType
import net.essentuan.esl.json.type.jsonType
import java.io.StringReader
import kotlin.reflect.KClass

abstract class JsonReaderAdapter<To : AnyJson>(val to: Class<To>) : JsonType.Adapter<JsonReader, To> {
    constructor(to: KClass<To>) : this(to.java)

    init {
        Adapters.register(StringAdapter())
    }

    override fun from(): Class<JsonReader> = JsonReader::class.java
    override fun to(): Class<To> = to

    @Suppress("UNCHECKED_CAST")
    override fun convert(obj: JsonReader): To =
        when (val value = read(obj)) {
            is AnyJson -> value as To
            is List<*> -> jsonType(this::empty) {
                "array" to value
            }
            else -> jsonType(this::empty) {
                "root" to value
            }
        }

    private fun read(reader: JsonReader): Any? {
        return when (val root = reader.peek()) {
            JsonToken.BEGIN_OBJECT -> jsonType(this::empty) {
                reader.beginObject()

                while (true) {
                    when (val token = reader.peek()) {
                        JsonToken.END_OBJECT -> break
                        JsonToken.NAME -> reader.nextName() to read(reader)
                        else -> error("Expected END_OBJECT or NAME but found $token")
                    }
                }

                reader.endObject()
            }

            JsonToken.BEGIN_ARRAY -> {
                reader.beginArray()

                val array = mutableListOf<Any?>()
                while (reader.peek() != JsonToken.END_ARRAY)
                    array += read(reader)

                reader.endArray()

                array
            }

            JsonToken.STRING -> reader.nextString()
            JsonToken.NUMBER -> LazilyParsedNumber(reader.nextString())
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NULL -> {
                reader.nextNull()

                null
            }
            else -> error("Unexpected token $root")
        }
    }

    abstract fun empty(): To

    inner class StringAdapter : JsonType.Adapter<String, To> {
        override fun from(): Class<String> = String::class.java

        override fun to(): Class<To> = this@JsonReaderAdapter.to

        override fun convert(obj: String): To =
            this@JsonReaderAdapter.convert(GsonTypes.DEFAULT.newJsonReader(StringReader(obj)))

    }
}