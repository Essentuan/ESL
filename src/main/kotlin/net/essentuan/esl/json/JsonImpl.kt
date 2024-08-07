package net.essentuan.esl.json

import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import net.essentuan.esl.iteration.extensions.mutable.iterate
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.json.type.GsonTypes
import net.essentuan.esl.json.type.MapBasedJsonType
import net.essentuan.esl.other.Repr
import net.essentuan.esl.other.repr
import java.io.StringWriter

internal class JsonImpl(
    map: MutableMap<String, Json.Value> = LinkedHashMap()
) : MapBasedJsonType<Json, Json.Value, Json.Entry>(map), Json {
    override fun Any?.checkThis(message: (String) -> String): Json {
        if (this is Json)
            return this

        throw IllegalArgumentException(message("Json"))
    }

    override fun getJson(key: String): Json? = this[key]?.asJson()

    override fun getJson(key: String, default: Json): Json = getJson(key) ?: default

    override fun empty(): Json = JsonImpl()

    override fun valueOf(obj: Any?): Json.Value = Value(obj)

    override fun entryOf(key: String, value: Json.Value): Json.Entry = Entry(key, value)

    override fun asString(prettyPrint: Boolean): String {
        val gson = if (prettyPrint) GsonTypes.PRETTY_PRINTING else GsonTypes.DEFAULT
        val out = StringWriter()
        val writer = gson.newJsonWriter(out)
        writer.serializeNulls = true

        write(this, gson, writer)

        return out.toString()
    }

    override fun toString(): String = repr {
        prefix(Json::class)

        with { Repr.Type.CURLY_BRACKETS }

        entries iterate {
            val value = it.raw

            it.key to if (value === this@JsonImpl)
                "(this)"
            else
                it.raw
        }
    }

    override fun hashCode(): Int = map.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other !is Json) return false

        return this === other || this.asMap() == other.asMap()
    }

    class Value(obj: Any?) : MapBasedJsonType.Value(obj), Json.Value {
        override fun asJson(): Json? = `as`(Json::class)
        override fun asJson(default: Json): Json = `as`(default)
    }

    inner class Entry(key: String, override val obj: Json.Value) :
        MapBasedJsonType<Json, Json.Value, Json.Entry>.Entry<Json.Value>(key, obj), Json.Entry, Json.Value by obj
}

internal fun write(value: Any?, gson: Gson, writer: JsonWriter) {
    when (value) {
        null -> writer.nullValue()
        is AnyJson -> {
            writer.beginObject()

            for (entry in value.entries) {
                writer.name(entry.key)

                write(entry.raw, gson, writer)
            }

            writer.endObject()
        }

        is Collection<*> -> {
            writer.beginArray()

            for (e in value)
                write(e, gson, writer)

            writer.endArray()
        }

        is String -> writer.value(value)
        is Int -> writer.value(value.toLong())
        is Long -> writer.value(value)
        is Float -> writer.value(value.toFloat())
        is Double -> writer.value(value)
        is Number -> writer.value(value)
        is Boolean -> writer.value(value)
        else -> gson.toJson(value, value.javaClass, writer)
    }
}