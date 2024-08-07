package net.essentuan.esl.time.duration

import net.essentuan.esl.time.TimeUnit
import java.util.function.BiFunction

fun interface FormatFlag {
    fun apply(formatter: Duration.Formatter)

    companion object {
        val COMPACT: FormatFlag = FormatFlag { formatter: Duration.Formatter ->
            formatter.suffix = BiFunction { _: Double, unit: TimeUnit -> "${unit.suffix()} "}
        }

        val MINIFIED: FormatFlag = FormatFlag { formatter: Duration.Formatter ->
            formatter.suffix = BiFunction { _: Double, unit: TimeUnit -> unit.suffix() }
        }
    }
}
