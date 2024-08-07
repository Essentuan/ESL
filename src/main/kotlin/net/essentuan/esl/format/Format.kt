package net.essentuan.esl.format

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode


private val THOUSAND = BigDecimal("1000")
private val MILLION = THOUSAND.multiply(THOUSAND)
private val BILLION = MILLION.multiply(THOUSAND)
private val TRILLION = BILLION.multiply(THOUSAND)
private val QUADRILLION = TRILLION.multiply(THOUSAND)

fun BigDecimal.truncate(): String {
    val (number, unit) = when {
        this < THOUSAND ->
            return divide(BigDecimal.ONE, 0, RoundingMode.HALF_UP).toString();
        this >= QUADRILLION -> divide(QUADRILLION, 3, RoundingMode.HALF_UP) to "Q"
        this >= TRILLION -> divide(TRILLION, 3, RoundingMode.HALF_UP) to "T"
        this >= BILLION -> divide(BILLION, 3, RoundingMode.HALF_UP) to "B"
        this >= MILLION -> divide(MILLION, 3, RoundingMode.HALF_UP) to "M"
        this >= THOUSAND -> divide(THOUSAND, 3, RoundingMode.HALF_UP) to "K"
        else -> throw IllegalArgumentException()
    }

    val scale = number.precision() - number.scale()

    return "${number.setScale(0.coerceAtLeast(4 - scale), RoundingMode.HALF_UP)}$unit"
}

fun Number.truncate(): String = when( this) {
    is BigDecimal -> this.truncate()
    is BigInteger -> this.toBigDecimal().truncate()
    else -> this.toDouble().toBigDecimal().truncate()
}