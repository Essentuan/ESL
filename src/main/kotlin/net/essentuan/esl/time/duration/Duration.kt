package net.essentuan.esl.time.duration

import com.google.common.collect.Lists
import net.essentuan.esl.comparing.Comparing
import net.essentuan.esl.encoding.AbstractEncoder
import net.essentuan.esl.other.Printable
import net.essentuan.esl.reflections.extensions.get
import net.essentuan.esl.reflections.extensions.tags
import net.essentuan.esl.string.extensions.indexOf
import net.essentuan.esl.string.extensions.isNumber
import net.essentuan.esl.time.NativeUnit
import net.essentuan.esl.time.TimeUnit
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit
import java.util.Date
import java.util.function.BiFunction
import java.util.regex.Pattern

typealias JavaDuration = java.time.Duration
typealias KDuration = kotlin.time.Duration

interface Duration : TemporalAmount, Comparable<Duration>, Printable {
    val isForever: Boolean

    operator fun plus(other: Duration): Duration

    fun plus(length: Double, unit: TimeUnit): Duration {
        return plus(Duration(length, unit))
    }

    fun add(other: Duration): Duration {
        return plus(other)
    }

    fun add(length: Double, unit: TimeUnit): Duration {
        return plus(length, unit)
    }

    operator fun minus(other: Duration): Duration

    fun minus(length: Double, unit: TimeUnit): Duration {
        return minus(Duration(length, unit))
    }

    fun subtract(other: Duration): Duration {
        return minus(other)
    }

    fun subtract(length: Double, unit: TimeUnit): Duration {
        return minus(length, unit)
    }

    fun multiply(other: Duration): Duration

    operator fun times(other: Duration): Duration {
        return multiply(other)
    }

    fun multiply(length: Double, unit: TimeUnit): Duration {
        return multiply(Duration(length, unit))
    }

    fun divide(other: Duration): Duration

    operator fun div(other: Duration): Duration {
        return divide(other)
    }

    fun divide(length: Double, unit: TimeUnit): Duration {
        return divide(Duration(length, unit))
    }

    fun mod(other: Duration): Duration

    operator fun rem(other: Duration): Duration {
        return mod(other)
    }

    fun mod(length: Double, unit: TimeUnit): Duration {
        return mod(Duration(length, unit))
    }

    fun pow(exponent: Duration): Duration

    infix fun raise(exponent: Duration): Duration {
        return pow(exponent)
    }

    fun pow(length: Double, unit: TimeUnit): Duration {
        return pow(Duration(length, unit))
    }

    infix fun min(other: Duration): Duration

    fun min(length: Double, unit: TimeUnit): Duration {
        return min(Duration(length, unit))
    }

    infix fun max(other: Duration): Duration

    fun max(length: Double, unit: TimeUnit): Duration {
        return max(Duration(length, unit))
    }

    fun abs(): Duration

    fun to(unit: TimeUnit): Double

    fun getPart(unit: TimeUnit): Double

    fun nanos(): Double {
        return getPart(TimeUnit.NANOSECONDS)
    }

    fun micros(): Double {
        return getPart(TimeUnit.MICROSECONDS)
    }

    fun mills(): Double {
        return getPart(TimeUnit.MILLISECONDS)
    }

    fun seconds(): Double {
        return getPart(TimeUnit.SECONDS)
    }

    fun minutes(): Double {
        return getPart(TimeUnit.MINUTES)
    }

    fun hours(): Double {
        return getPart(TimeUnit.HOURS)
    }

    fun days(): Double {
        return getPart(TimeUnit.DAYS)
    }

    fun weeks(): Double {
        return getPart(TimeUnit.WEEKS)
    }

    fun months(): Double {
        return getPart(TimeUnit.MONTHS)
    }

    fun years(): Double {
        return getPart(TimeUnit.YEARS)
    }

    fun greaterThan(other: Duration): Boolean

    fun greaterThan(length: Double, unit: TimeUnit): Boolean {
        return greaterThan(Duration(length, unit))
    }

    fun greaterThanOrEqual(other: Duration): Boolean

    fun greaterThanOrEqual(length: Double, unit: TimeUnit): Boolean {
        return greaterThanOrEqual(Duration(length, unit))
    }

    fun lessThan(other: Duration): Boolean

    fun lessThan(length: Double, unit: TimeUnit): Boolean {
        return lessThan(Duration(length, unit))
    }

    fun lessThanOrEqual(other: Duration): Boolean

    fun lessThanOrEqual(length: Double, unit: TimeUnit): Boolean {
        return lessThanOrEqual(Duration(length, unit))
    }

    fun equals(length: Double, unit: TimeUnit): Boolean {
        return equals(Duration(length, unit))
    }

    fun print(vararg flags: FormatFlag): String {
        return Formatter(this, *flags).toString()
    }

    override fun print(): String = print(*emptyArray())

    fun toJava(): JavaDuration

    fun toKotlin(): KDuration

    fun toNanos(): Double {
        return to(TimeUnit.NANOSECONDS)
    }

    fun toMicros(): Double {
        return to(TimeUnit.MICROSECONDS)
    }

    fun toMills(): Double {
        return to(TimeUnit.MILLISECONDS)
    }

    fun toSeconds(): Double {
        return to(TimeUnit.SECONDS)
    }

    fun toMinutes(): Double {
        return to(TimeUnit.MINUTES)
    }

    fun toHours(): Double {
        return to(TimeUnit.HOURS)
    }

    fun toDays(): Double {
        return to(TimeUnit.DAYS)
    }

    fun toWeeks(): Double {
        return to(TimeUnit.WEEKS)
    }

    fun toMonths(): Double {
        return to(TimeUnit.MONTHS)
    }

    fun toYears(): Double {
        return to(TimeUnit.YEARS)
    }

    class Formatter(duration: Duration, vararg flags: FormatFlag) {
        var smallestUnit: TimeUnit = TimeUnit.MILLISECONDS

        var suffix: BiFunction<Double, TimeUnit, String> =
            BiFunction<Double, TimeUnit, String> { length: Double, unit: TimeUnit ->
                val suffix = when (unit) {
                    TimeUnit.NANOSECONDS -> " nanosecond"
                    TimeUnit.MICROSECONDS -> " microsecond"
                    TimeUnit.MILLISECONDS -> " millisecond"
                    TimeUnit.SECONDS -> " second"
                    TimeUnit.MINUTES -> " minute"
                    TimeUnit.HOURS -> " hour"
                    TimeUnit.DAYS -> " day"
                    TimeUnit.WEEKS -> " week"
                    TimeUnit.MONTHS -> " month"
                    TimeUnit.YEARS -> " year"
                }
                (if (length > 1) suffix + "s" else suffix) + " "
            }

        val duration: Duration

        init {
            for (flag in flags) {
                flag.apply(this)
            }

            this.duration = duration
        }

        override fun toString(): String {
            if (duration.isForever) return "Forever"
            else if (duration.lessThan(1.0, smallestUnit)) return ("0" + suffix.apply(
                0.0,
                smallestUnit
            )).trim { it <= ' ' }

            val units = Lists.reverse(listOf(*TimeUnit.entries.toTypedArray()))

            val builder = StringBuilder()

            for (unit in units) {
                val value = duration.getPart(unit)

                if (value != 0.0 && !isForever(value)) {
                    builder.append(value.toInt())
                        .append(suffix.apply(value, unit))
                }

                if (unit == smallestUnit) {
                    return builder.toString().trim { it <= ' ' }
                }
            }

            return builder.toString().trim { it <= ' ' }
        }
    }

    companion object {
        fun isForever(double: Double): Boolean {
            return double.isNaN() || double == Double.NEGATIVE_INFINITY || double == Double.POSITIVE_INFINITY
        }

        operator fun invoke(): Duration =
            0.seconds

        operator fun invoke(length: Double, unit: TimeUnit): Duration {
            return Impl(length * unit.seconds())
        }

        operator fun invoke(length: Number, unit: TimeUnit): Duration {
            return invoke(length.toDouble(),  unit)
        }


        operator fun invoke(length: Double, unit: NativeUnit): Duration {
            return invoke(length, TimeUnit.from(unit))
        }

        operator fun invoke(start: Date, end: Date): Duration {
            return invoke((end.time - start.time), TimeUnit.MILLISECONDS)
        }

        operator fun invoke(duration: JavaDuration): Duration {
            return duration.seconds.seconds + duration.nano.nanos
        }

        fun since(date: Date): Duration {
            return invoke(date, Date())
        }

        fun until(date: Date): Duration {
            return invoke(Date(), date)
        }

        fun compare(duration1: Duration?, duration2: Duration?): Int {
            return Comparing.compare(duration1, duration2)
        }

        @Suppress("CheckedExceptionsKotlin")
        operator fun invoke(string: String): Duration? {
            var string = string
            var index: Int

            string = string.replace(" ", "").lowercase()

            val pattern: Pattern = TimeUnit.regex()

            var duration = 0.seconds

            while (string.indexOf(pattern).also { index = it } != -1) {
                for (unit in TimeUnit.sorted()) {
                    val length: Int = when {
                        string.startsWith(unit.plural().lowercase(), index) -> unit.plural().length
                        string.startsWith(unit.singular().lowercase(), index) -> unit.singular().length
                        string.startsWith(unit.suffix(), index) -> unit.suffix().length
                        else -> continue
                    }

                    val number = string.substring(0, index)

                    if (!number.isNumber())
                        return null

                    duration+= number.toDouble() of unit

                    string = string.substring(index + length)

                    break
                }
            }
            return if (duration <= 0.seconds) null else duration
        }

        val FOREVER: Duration by lazy { Double.NaN.seconds }
    }

    interface Helper : Duration {
        val duration: Duration

        override val isForever: Boolean
            get() = duration.isForever

        override fun plus(other: Duration): Duration =
            duration + other
        override fun minus(other: Duration): Duration =
            duration - other

        override fun multiply(other: Duration): Duration =
            duration * other

        override fun divide(other: Duration): Duration =
            duration / other

        override fun mod(other: Duration): Duration =
            duration % other

        override fun pow(exponent: Duration): Duration =
            duration raise exponent

        override fun min(other: Duration): Duration =
            duration min other

        override fun max(other: Duration): Duration =
            duration max other

        override fun abs(): Duration =
            duration.abs()

        override fun to(unit: TimeUnit): Double =
            duration.to(unit)

        override fun getPart(unit: TimeUnit): Double =
            duration.getPart(unit)

        override fun greaterThan(other: Duration): Boolean =
            duration > other

        override fun greaterThanOrEqual(other: Duration): Boolean =
            duration >= other

        override fun lessThan(other: Duration): Boolean =
            duration < other

        override fun lessThanOrEqual(other: Duration): Boolean =
            duration <= other

        override fun toJava(): JavaDuration =
            duration.toJava()

        override fun toKotlin(): KDuration =
            duration.toKotlin()

        override fun get(unit: TemporalUnit?): Long =
            duration.get(unit)

        override fun getUnits(): MutableList<TemporalUnit> =
            duration.units

        override fun addTo(temporal: Temporal?): Temporal =
            duration.addTo(temporal)

        override fun subtractFrom(temporal: Temporal?): Temporal =
            duration.subtractFrom(temporal)

        override fun compareTo(other: Duration): Int =
            duration.compareTo(other)
    }
}

object Encoder : AbstractEncoder<Duration, Number>() {
    override fun encode(obj: Duration, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Number =
        obj.to(element.tags[net.essentuan.esl.time.Unit::class]?.value ?: TimeUnit.SECONDS)

    override fun decode(obj: Number, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Duration =
        Duration(obj, (element.tags[net.essentuan.esl.time.Unit::class]?.value ?: TimeUnit.SECONDS))

    override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Duration =
        decode(string.toDouble(), flags, type, element, *typeArgs)

    override fun toString(obj: Duration, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        encode(obj, flags, type, element, *typeArgs).toString()
}

inline fun timeit(action: () -> Unit): Duration {
    val start = Date()

    action()

    return Duration.since(start)
}

inline fun timeit(trials: Int, action: () -> Unit): Duration {
    val start = Date()

    for (i in 0..trials)
        action()

    return Duration.since(start)
}

infix fun Number.of(unit: TimeUnit): Duration = Duration(this, unit)

val Number.nanos: Duration
    get() = this of TimeUnit.NANOSECONDS

val Number.micros: Duration
    get() = this of TimeUnit.MICROSECONDS

val Number.ms: Duration
    get() = this of TimeUnit.MILLISECONDS

val Number.seconds: Duration
    get() = this of TimeUnit.SECONDS

val Number.minutes: Duration
    get() = this of TimeUnit.MINUTES

val Number.hours: Duration
    get() = this of TimeUnit.HOURS

val Number.days: Duration
    get() = this of TimeUnit.DAYS

val Number.weeks: Duration
    get() = this of TimeUnit.WEEKS

val Number.months: Duration
    get() = this of TimeUnit.MONTHS

val Number.years: Duration
    get() = this of TimeUnit.YEARS