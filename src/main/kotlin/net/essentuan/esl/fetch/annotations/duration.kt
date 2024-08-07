package net.essentuan.esl.fetch.annotations

import net.essentuan.esl.time.duration.days
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.hours
import net.essentuan.esl.time.duration.micros
import net.essentuan.esl.time.duration.ms
import net.essentuan.esl.time.duration.minutes
import net.essentuan.esl.time.duration.months
import net.essentuan.esl.time.duration.nanos
import net.essentuan.esl.time.duration.seconds
import net.essentuan.esl.time.duration.weeks
import net.essentuan.esl.time.duration.years

annotation class duration(
    val years: Double = 0.0,
    val months: Double = 0.0,
    val weeks: Double = 0.0,
    val days: Double = 0.0,
    val hours: Double = 0.0,
    val minutes: Double = 0.0,
    val seconds: Double = 0.0,
    val ms: Double = 0.0,
    val us: Double = 0.0,
    val ns: Double = 0.0
)

fun duration.duration(): Duration {
    return years.years +
            months.months +
            weeks.weeks +
            days.days +
            hours.hours +
            minutes.minutes +
            seconds.seconds +
            ms.ms +
            us.micros +
            ns.nanos
}