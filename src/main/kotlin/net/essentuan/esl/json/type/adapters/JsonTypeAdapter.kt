package net.essentuan.esl.json.type.adapters

import net.essentuan.esl.iteration.extensions.mutable.iterate
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.json.type.JsonType
import kotlin.reflect.KClass

abstract class JsonTypeAdapter<To: AnyJson>(val to: Class<To>): JsonType.Adapter<AnyJson, To> {
    constructor(to: KClass<To>): this(to.java)

    override fun from(): Class<AnyJson> = JsonType::class.java
    override fun to(): Class<To> = to

    @Suppress("UNCHECKED_CAST")
    override fun convert(obj: AnyJson): To {
        val result = empty()

        obj.entries iterate {
            when {
                it `is` to -> result[it.key] = it.raw
                it `is` JsonType::class -> convert(it.`as`(JsonType::class)!!)
                else -> result[it.key] = it.raw
            }
        }

        return result
    }

    abstract fun empty(): To
}