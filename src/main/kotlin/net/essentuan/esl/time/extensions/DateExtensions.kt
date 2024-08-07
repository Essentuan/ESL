package net.essentuan.esl.time.extensions

import com.github.sisyphsu.dateparser.DateParserUtils
import net.essentuan.esl.time.TimeUnit
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.span.TimeSpan
import java.util.Date

fun Date.timeSince(): Duration = Duration.since(this)

fun Date.timeUntil(): Duration = Duration.until(this)

infix fun Date.to(other: Date): Duration = Duration(this, other)

operator fun Date.plus(duration: Duration): Date = Date(time + duration.to(TimeUnit.MILLISECONDS).toLong())

operator fun Date.minus(duration: Duration): Date = Date(time - duration.to(TimeUnit.MILLISECONDS).toLong())

operator fun Date.rangeTo(other: Date): TimeSpan = TimeSpan(this, other)

fun String.toDate(): Date {
    return try {
        Date(toLong())
    } catch(ex: NumberFormatException) {
        DateParserUtils.parseDate(this)
    }
}