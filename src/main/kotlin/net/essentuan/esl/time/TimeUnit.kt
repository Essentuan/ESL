package net.essentuan.esl.time

import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.FormatFlag
import java.time.temporal.Temporal
import java.time.temporal.TemporalUnit
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream

typealias NativeUnit = java.util.concurrent.TimeUnit

enum class TimeUnit(private val seconds: Double, private val suffix: String) : TemporalUnit, FormatFlag {
    NANOSECONDS(1.0 / 1_000_000_000, "ns"),
    MICROSECONDS(1.0 / 1_000_000, "us"),
    MILLISECONDS(1.0 / 1_000, "ms"),
    SECONDS(1.0, "s"),
    MINUTES(SECONDS.seconds() * 60, "m"),
    HOURS(MINUTES.seconds() * 60, "h"),
    DAYS(HOURS.seconds() * 24, "d"),
    WEEKS(DAYS.seconds() * 7, "w"),
    MONTHS(WEEKS.seconds() * 4.345238095238096, "mo"),
    YEARS(MONTHS.seconds() * 12, "y");

    override fun getDuration(): java.time.Duration {
        return Duration(1.0, this).toJava()
    }

    fun seconds(): Double {
        return seconds
    }

    fun suffix(): String {
        return suffix
    }

    fun plural(): String {
        return name
    }

    fun singular(): String {
        return name.substring(0, name.length - 1)
    }

    override fun isDurationEstimated(): Boolean {
        return false
    }

    override fun isDateBased(): Boolean {
        return false
    }

    override fun isTimeBased(): Boolean {
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <R : Temporal> addTo(temporal: R, amount: Long): R {
        return temporal.plus(Duration(seconds() * amount, SECONDS)) as R
    }

    override fun between(temporal1Inclusive: Temporal, temporal2Exclusive: Temporal): Long {
        return temporal1Inclusive.until(temporal2Exclusive, this)
    }

    override fun apply(formatter: Duration.Formatter) {
        formatter.smallestUnit = this
    }

    companion object {
        private val SORTED: List<TimeUnit> = Stream.of(*entries.toTypedArray())
            .sorted(Comparator.comparing { unit: TimeUnit -> unit.suffix().length }
                .reversed())
            .toList()

        fun regex(): Pattern {
            return Pattern.compile(SORTED.stream()
                .map { unit: TimeUnit -> "((${unit.plural()})|(${unit.singular()})|(${unit.suffix}))" }
                .collect(Collectors.joining("|")))
        }

        fun sorted(): List<TimeUnit> {
            return SORTED
        }

        fun native(unit: TimeUnit): NativeUnit {
            return when (unit) {
                NANOSECONDS -> NativeUnit.NANOSECONDS
                MICROSECONDS -> NativeUnit.MICROSECONDS
                MILLISECONDS -> NativeUnit.MILLISECONDS
                SECONDS -> NativeUnit.SECONDS
                MINUTES -> NativeUnit.MINUTES
                HOURS -> NativeUnit.HOURS
                DAYS -> NativeUnit.DAYS
                else -> error("Unexpected value: $unit")
            }
        }

        fun from(unit: NativeUnit): TimeUnit {
            return when (unit) {
                NativeUnit.NANOSECONDS -> NANOSECONDS
                NativeUnit.MICROSECONDS -> MICROSECONDS
                NativeUnit.MILLISECONDS -> MILLISECONDS
                NativeUnit.SECONDS -> SECONDS
                NativeUnit.MINUTES -> MINUTES
                NativeUnit.HOURS -> HOURS
                NativeUnit.DAYS -> DAYS
            }
        }
    }
}