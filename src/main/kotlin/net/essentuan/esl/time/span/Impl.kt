package net.essentuan.esl.time.span

import net.essentuan.esl.time.TimeUnit
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.FormatFlag
import net.essentuan.esl.time.duration.JavaDuration
import net.essentuan.esl.time.duration.KDuration
import net.essentuan.esl.time.extensions.minus
import net.essentuan.esl.time.extensions.plus
import java.time.temporal.Temporal
import java.time.temporal.TemporalUnit
import java.util.Date

internal class Impl(override val start: Date, private val length: Duration) : TimeSpan {
    override val end: Date = start + length

    override fun shr(other: Duration): TimeSpan = Impl(start + other, length)

    override fun shl(other: Duration): TimeSpan = Impl(start - other, length)

    override fun contains(date: Date): Boolean = start.time <= date.time && end.time >= date.time

    private inline fun make(other: Duration, operator: (Duration, Duration) -> Duration): TimeSpan {
        return Impl(start, operator(length, other))
    }

    override fun plus(other: Duration): TimeSpan = make(other, Duration::plus)

    override fun minus(other: Duration): TimeSpan = make(other, Duration::minus)

    override fun multiply(other: Duration): TimeSpan = make(other, Duration::multiply)

    override fun divide(other: Duration): TimeSpan = make(other, Duration::divide)

    override fun mod(other: Duration): TimeSpan = make(other, Duration::mod)

    override fun pow(exponent: Duration): TimeSpan = make(exponent, Duration::pow)

    override fun abs(): TimeSpan = Impl(start, length.abs())

    override fun min(other: Duration): Duration = length.min(other)

    override fun max(other: Duration): Duration = length.max(other)

    override fun to(unit: TimeUnit): Double = length.to(unit)

    override fun getPart(unit: TimeUnit): Double = length.getPart(unit)

    override fun greaterThan(other: Duration): Boolean = length.greaterThan(other)

    override fun greaterThanOrEqual(other: Duration): Boolean = length.greaterThanOrEqual(other)

    override fun lessThan(other: Duration): Boolean = length.lessThan(other)

    override fun lessThanOrEqual(other: Duration): Boolean = length.lessThanOrEqual(other)

    override val isForever: Boolean
        get() = length.isForever

    override fun toJava(): JavaDuration = length.toJava()

    override fun toKotlin(): KDuration = length.toKotlin()

    override fun get(unit: TemporalUnit?): Long = length[unit]

    override fun getUnits(): MutableList<TemporalUnit> = length.units

    override fun addTo(temporal: Temporal?): Temporal = length.addTo(temporal)

    override fun subtractFrom(temporal: Temporal?): Temporal = length.subtractFrom(temporal)

    override fun compareTo(other: Duration): Int = length.compareTo(other)

    override fun print(vararg flags: FormatFlag): String = length.print(*flags)
}