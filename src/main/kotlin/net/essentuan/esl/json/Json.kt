package net.essentuan.esl.json

import net.essentuan.esl.json.type.AbstractBuilder
import net.essentuan.esl.json.type.JsonType
import net.essentuan.esl.json.type.adapters.GsonAdapter
import net.essentuan.esl.json.type.adapters.JsonReaderAdapter
import net.essentuan.esl.json.type.adapters.JsonTypeAdapter
import net.essentuan.esl.model.Extension
import net.essentuan.esl.model.field.Parameter
import net.essentuan.esl.model.field.Property
import net.essentuan.esl.model.impl.ParameterImpl
import net.essentuan.esl.model.impl.PropertyImpl
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty

interface Json : JsonType<Json, Json.Value, Json.Entry> {
    fun getJson(key: String): Json?

    fun getJson(key: String, default: Json): Json

    fun isJson(key: String): Boolean = this[key]?.isJson() == true

    fun asString(prettyPrint: Boolean = false): String

    interface Value : JsonType.Value {
        fun isJson(): Boolean = raw is Json

        fun asJson(): Json?

        fun asJson(default: Json): Json
    }

    interface Entry : JsonType.Entry, Value

    companion object {
        init {
            JsonType.register(
                object : JsonTypeAdapter<Json>(Json::class) {
                    override fun empty(): Json = Json()
                },
                object : GsonAdapter<Json>(Json::class) {
                    override fun empty(): Json = Json()
                },
                object : JsonReaderAdapter<Json>(Json::class) {
                    override fun empty(): Json = Json()
                }
            )
        }

        operator fun invoke(): Json = JsonImpl()

        operator fun invoke(obj: Any): Json = JsonType.valueOf(obj)
    }

    class Builder(json: Json = Json()) : AbstractBuilder<Json, Value, Entry>(json) {
        override fun empty(): Json = Json()

        override fun builder(): AbstractBuilder<Json, Value, Entry> = Builder()
    }

    interface Model : net.essentuan.esl.model.Model<Json> {
        companion object : Extension<Model> {
            override fun invoke(param: KParameter): Parameter =
                object : ParameterImpl(param) {}

            override fun invoke(property: KProperty<*>): Property =
                object : PropertyImpl(property) {}
        }
    }
}

fun json(base: Json, init: Json.Builder.() -> Unit): Json = Json.Builder(base).apply(init).jsonType

fun json(init: Json.Builder.() -> Unit): Json = json(Json(), init)

fun String.toJson(): Json = JsonType.valueOf(this)