package net.essentuan.esl.other

fun <T: Comparable<T>> T.clamp(range: ClosedRange<T>): T = this.clamp(range.start, range.endInclusive)

fun <T: Comparable<T>> T.clamp(min: T, max: T): T = when {
    this < min -> min
    this > max -> max
    else -> this
}