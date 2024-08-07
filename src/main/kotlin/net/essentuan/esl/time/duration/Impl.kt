package net.essentuan.esl.time.duration

import net.essentuan.esl.other.repr
import net.essentuan.esl.time.TimeUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalUnit
import java.util.stream.Stream
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.toKotlinDuration

internal val Duration.seconds: Double
    get() = if (this is Impl) this.seconds else toSeconds()

internal class Impl(internal val seconds: Double) : Duration {
    override val isForever: Boolean
        get() = Duration.isForever(seconds)

    private inline fun operator(other: Duration, operator: (Double, Double) -> Double): Duration {
        return Impl(operator(seconds, other.seconds))
    }

    override fun plus(other: Duration): Duration {
        return operator(other) { a: Double, b: Double -> a + b }
    }

    override fun minus(other: Duration): Duration {
        return operator(other) { left: Double, right: Double -> left - right }
    }

    override fun multiply(other: Duration): Duration {
        return operator(other) { left: Double, right: Double -> left * right }
    }

    override fun divide(other: Duration): Duration {
        return operator(other) { left: Double, right: Double -> left / right }
    }

    override fun mod(other: Duration): Duration {
        return operator(other) { left: Double, right: Double -> left % right }
    }

    override fun pow(exponent: Duration): Duration {
        return operator(exponent) { a: Double, b: Double -> a.pow(b) }
    }

    override fun min(other: Duration): Duration {
        return if (lessThan(other)) this else other
    }

    override fun max(other: Duration): Duration {
        return if (greaterThan(other)) this else other
    }

    override fun abs(): Duration {
        return Impl(kotlin.math.abs(seconds))
    }

    override fun to(unit: TimeUnit): Double {
        return seconds / unit.seconds()
    }

    override fun toSeconds(): Double = seconds

    override fun getPart(unit: TimeUnit): Double {
        return floor(
            when (unit) {
                TimeUnit.NANOSECONDS -> toNanos() % 1000
                TimeUnit.MICROSECONDS -> toMicros() % 1000
                TimeUnit.MILLISECONDS -> toMills() % 1000
                TimeUnit.SECONDS -> toSeconds() % 60
                TimeUnit.MINUTES -> toMinutes() % 60
                TimeUnit.HOURS -> toHours() % 24
                TimeUnit.DAYS -> toDays() % 7
                TimeUnit.WEEKS -> toWeeks() % 4.345238095238096
                TimeUnit.MONTHS -> toMonths() % 12
                TimeUnit.YEARS -> toYears()
            }
        )
    }

    override fun greaterThan(other: Duration): Boolean {
        return seconds > other.seconds
    }

    override fun greaterThanOrEqual(other: Duration): Boolean {
        return seconds >= other.seconds
    }

    override fun lessThan(other: Duration): Boolean {
        return seconds < other.seconds
    }

    override fun lessThanOrEqual(other: Duration): Boolean {
        return seconds <= other.seconds
    }

    override fun toJava(): java.time.Duration {
        val seconds = min(toSeconds(), Long.MAX_VALUE.toDouble()).toLong()
        val nanos = min((this.seconds - seconds) / TimeUnit.NANOSECONDS.seconds(), 999_999_999.0).toLong()

        return JavaDuration.ofSeconds(seconds, nanos)
    }

    override fun toKotlin(): KDuration = toJava().toKotlinDuration()

    override fun get(unit: TemporalUnit): Long {
        return (seconds / unit.duration.toSeconds()).toLong()
    }

    override fun getUnits(): MutableList<TemporalUnit> =
        Stream.of(*TimeUnit.entries.toTypedArray())
            .filter { unit: TimeUnit -> getPart(unit) != 0.0 && !Duration.isForever(getPart(unit)) }
            .map { obj: TimeUnit -> TemporalUnit::class.java.cast(obj) }
            .toList()

    override fun addTo(temporal: Temporal): Temporal {
        return temporal.plus(this)
    }

    override fun subtractFrom(temporal: Temporal): Temporal {
        return temporal.minus(this)
    }

    override fun toString(): String = repr {
        prefix(Duration::class)

        "seconds" to seconds
    }

    override fun compareTo(other: Duration): Int {
        return seconds.compareTo(other.toSeconds())
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is Duration && seconds == other.toSeconds())
    }

    override fun hashCode(): Int {
        return seconds.hashCode()
    }
}
