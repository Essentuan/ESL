package net.essentuan.esl.time.extensions

import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.json.type.JsonType
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.ms
import net.essentuan.esl.time.duration.nanos
import net.essentuan.esl.time.duration.seconds

fun JsonType.Value.isDuration(): Boolean {
    return when {
        `is`(Duration::class) -> true
        `is`(JsonType::class) -> {
            val json = `as`(JsonType::class)!!

            return json.has("seconds")
        }
        isNumber() -> true
        isNull() -> false
        else -> throw ClassCastException("${raw?.javaClass?.simpleString()} cannot be cast to ${Duration::class.simpleString()}")
    }
}

fun JsonType.Value.asDuration(): Duration? {
    return when {
        `is`(Duration::class) -> `as`(Duration::class)
        `is`(JsonType::class) && `as`(JsonType::class)!!.has("seconds") -> {
            val json = `as`(JsonType::class)!!

            return (json.getDouble("seconds")!!.seconds) + (json.getDouble(
                "nanos",
                0.0
            ).nanos)
        }

        isNumber() -> asDouble()!!.ms
        isNull() -> null
        else -> throw ClassCastException("${raw?.javaClass?.simpleString()} cannot be cast to ${Duration::class.simpleString()}")
    }
}

fun JsonType.Value.asDuration(default: Duration) = asDuration() ?: default

fun AnyJson.isDuration(key: String): Boolean = this[key]?.isDuration() == true

fun AnyJson.getDuration(key: String): Duration? = this[key]?.asDuration()

fun AnyJson.getDuration(key: String, default: Duration): Duration = getDuration(key) ?: default