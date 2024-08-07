package net.essentuan.esl.model

import net.essentuan.esl.json.type.AnyJson

fun interface Factory<T: Model<JSON>, JSON: AnyJson> {
    operator fun invoke(json: JSON, flags: Set<Any>): T
}