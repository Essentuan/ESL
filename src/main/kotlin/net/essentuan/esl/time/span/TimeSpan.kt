package net.essentuan.esl.time.span

import net.essentuan.esl.encoding.AbstractEncoder
import net.essentuan.esl.json.json
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.time.TimeUnit
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.FormatFlag
import net.essentuan.esl.time.duration.of
import net.essentuan.esl.time.duration.seconds
import net.essentuan.esl.time.extensions.minus
import net.essentuan.esl.time.extensions.plus
import net.essentuan.esl.time.extensions.rangeTo
import net.essentuan.esl.time.extensions.timeSince
import net.essentuan.esl.time.extensions.to
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.util.Date

interface TimeSpan : Duration {
    val start: Date

    /**
     * Represents the end of the date. A null value indicates this [TimeSpan] is ongoing
     */
    val end: Date?

    /**
     * Offsets the [start] & [end] of this [TimeSpan] forward by the given [Duration]
     *
     * @return The offset[TimeSpan]
     * @see shl
     */
    infix fun shr(other: Duration): TimeSpan

    /**
     * Offsets the [start] & [end] of this [TimeSpan] backward by the given [Duration]
     *
     * @return The offset [TimeSpan]
     * @see shr
     */
    infix fun shl(other: Duration): TimeSpan

    /**
     * Checks to see if the date is between [start] (Inclusive) and [end] (Inclusive)
     *
     *
     * @return true if the date is contained within the [TimeSpan]
     */
    operator fun contains(date: Date): Boolean

    override operator fun plus(other: Duration): TimeSpan

    override fun plus(length: Double, unit: TimeUnit): TimeSpan = plus(Duration(length, unit))

    override fun add(other: Duration): TimeSpan = plus(other)

    override fun add(length: Double, unit: TimeUnit): TimeSpan = plus(length, unit)

    override operator fun minus(other: Duration): TimeSpan

    override fun minus(length: Double, unit: TimeUnit): TimeSpan = minus(Duration(length, unit))

    override fun subtract(other: Duration): TimeSpan = minus(other)

    override fun subtract(length: Double, unit: TimeUnit): TimeSpan = minus(length, unit)

    override fun multiply(other: Duration): TimeSpan

    override operator fun times(other: Duration): TimeSpan = multiply(other)

    override fun multiply(length: Double, unit: TimeUnit): TimeSpan = multiply(Duration(length, unit))

    override fun divide(other: Duration): TimeSpan

    override operator fun div(other: Duration): TimeSpan = divide(other)

    override fun divide(length: Double, unit: TimeUnit): TimeSpan = divide(Duration(length, unit))

    override fun mod(other: Duration): TimeSpan

    override operator fun rem(other: Duration): TimeSpan = mod(other)

    override fun mod(length: Double, unit: TimeUnit): TimeSpan = mod(Duration(length, unit))

    override fun pow(exponent: Duration): TimeSpan

    override infix fun raise(exponent: Duration): TimeSpan = pow(exponent)

    override fun pow(length: Double, unit: TimeUnit): TimeSpan = pow(Duration(length, unit))

    override fun abs(): TimeSpan

    fun suffix(minimum: Duration = 1.seconds, vararg flags: FormatFlag): String {
        val end = end?.timeSince()

        if (end == null || end < minimum)
            return "in the past ${print(*flags)}"

        val smallest = 1 of (flags.find { f -> f is TimeUnit } as TimeUnit? ?: TimeUnit.MILLISECONDS)

        fun Duration.relative(): String =
            if (this < smallest) "now" else "${print(*flags)} ago"

        val start = start.timeSince()

        return "between ${start.relative()} and ${end.relative()}"
    }

    companion object {
        operator fun invoke(start: Date, length: Duration): TimeSpan {
            return Impl(start, length)
        }

        operator fun invoke(start: Date, length: Double, unit: TimeUnit): TimeSpan {
            return TimeSpan(start, length of unit)
        }

        operator fun invoke(start: Date, end: Date): TimeSpan {
            return if (start.time > end.time)
                TimeSpan(end, end to start)
            else
                TimeSpan(start, start to end)
        }

        operator fun invoke(start: Duration, end: Duration): TimeSpan {
            val current = Date().time

            return TimeSpan(
                Date(current - start.toMills().toLong()),
                Date(current - end.toMills().toLong())
            )
        }

        fun past(duration: Duration): TimeSpan {
            return TimeSpan(duration, 0.seconds)
        }

        fun past(length: Double, unit: TimeUnit): TimeSpan {
            return past(length of unit)
        }

        fun before(date: Date): TimeSpan {
            return TimeSpan(Date(0), date)
        }
    }

    interface Helper : TimeSpan, Duration.Helper {
        override fun shr(other: Duration): TimeSpan =
            TimeSpan(start + other, duration)

        override fun shl(other: Duration): TimeSpan =
            TimeSpan(start - other, duration)

        override fun contains(date: Date): Boolean =
            start.time <= date.time && (end ?: date).time >= date.time

        override fun plus(other: Duration): TimeSpan =
            TimeSpan(start, duration + other)

        override fun minus(other: Duration): TimeSpan =
            TimeSpan(start, duration - other)

        override fun multiply(other: Duration): TimeSpan =
            TimeSpan(start, duration * other)

        override fun divide(other: Duration): TimeSpan =
            TimeSpan(start, duration / other)

        override fun mod(other: Duration): TimeSpan =
            TimeSpan(start, duration % other)

        override fun pow(exponent: Duration): TimeSpan =
            TimeSpan(start, duration raise exponent)

        override fun abs(): TimeSpan =
            TimeSpan(start, duration.abs())
    }
}

object Encoder : AbstractEncoder<TimeSpan, AnyJson>() {
    override fun encode(obj: TimeSpan, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): AnyJson =
        json {
            "start" to obj.start.time
            "end" to (obj.end?.time ?: System.currentTimeMillis())
        }

    override fun decode(obj: AnyJson, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): TimeSpan =
        obj.getDate("start")!!..obj.getDate("end")!!

    override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): TimeSpan {
        val parts = string.split("|")

        return Date(parts[0].toLong())..Date(parts[1].toLong())
    }

    override fun toString(obj: TimeSpan, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        "${obj.start.time}|${obj.end?.time ?: System.currentTimeMillis()}"
}
