package net.essentuan.esl.string.extensions

import net.essentuan.esl.extensions.ceq
import java.util.Collections
import java.util.UUID
import java.util.regex.Matcher
import java.util.regex.Pattern

fun <T : Any> String.bestMatch(
    iterable: Iterable<T>,
    mapper: (T) -> Array<String> = { arrayOf(it.toString()) },
    caseSensitive: Boolean = false
): T? = bestMatch(iterable.asSequence(), mapper, caseSensitive)

fun <T : Any> String.bestMatch(
    sequence: Sequence<T>,
    mapper: (T) -> Array<String> = { arrayOf(it.toString()) },
    caseSensitive: Boolean = false
): T? {
    var result: T? = null
    var score: Double = -1.0

    for (e in sequence) {
        val options = mapper(e)

        for (option in options) {
            if (option.length != length) continue

            if (option == this)
                return e

            if (caseSensitive) continue

            var matches = 0.0

            for (i in indices) {
                val char = this[i]

                if (char == option[i]) matches += 1.0
                else if (char ceq option[i]) matches += 0.5
                else matches -= 1
            }

            if (score == -1.0 || score < matches) {
                result = e
                score = matches
            }
        }
    }

    return result
}

fun String.isNumber(): Boolean {
    return when {
        isEmpty() -> false
        this[length - 1] == '.' -> false
        this[0] == '-' -> (length != 1) && withDecimalsParsing(this, 1)
        else -> withDecimalsParsing(this, 0)
    }
}

fun String.indexOf(pattern: Pattern): Int {
    val matcher: Matcher = pattern.matcher(this)
    return if (matcher.find()) matcher.start() else -1
}

fun String.truncate(chars: Int): String {
    return if (length > chars) "${substring(0, chars)}..." else this
}

fun String.nCopies(copies: Int): String {
    return Collections.nCopies(copies, this).joinToString { "" }
}

private fun isHyphen(index: Int): Boolean {
    return index == 8 || index == 13 || index == 18 || index == 23
}

/**
 * Validates if a string is a UUID using the follow formats:
 *
 * 306afda9-eaef-4ac5-a9fb-0ae83ab5adc0
 * 306afda9eaef4ac5a9fb0ae83ab5adc0
 *
 * @return true if [String] is a valid UUID
 */
fun String.isUUID(): Boolean {
    if (length != 36 && length != 32)
        return false

    for (i in indices) {
        when (this[i]) {
            '-' -> if (length == 36 && isHyphen(i)) continue
            in 'a'..'z',
            in 'A'..'Z',
            in '0'..'9' -> continue
        }

        return false
    }

    return true
}

fun String.toUUID(): UUID {
    if (length == 36)
        return UUID.fromString(this)

    require(length == 32) {
        { "UUID string length is $length not 32!" }
    }

    val chars = toCharArray()
    val result = CharArray(36)

    result[8] = '-'
    result[13] = '-'
    result[18] = '-'
    result[23] = '-'

    System.arraycopy(chars, 0, result, 0, 8)
    System.arraycopy(chars, 8, result, 9, 4)
    System.arraycopy(chars, 12, result, 14, 4)
    System.arraycopy(chars, 16, result, 19, 4)
    System.arraycopy(chars, 20, result, 24, 12)

    return UUID.fromString(String(result))
}

fun String.camelCase(
    first: Boolean = true,
    separator: String = "",
    vararg delimiters: Char = charArrayOf('_', ' '),
): String =
    buildString {
        val result = this@camelCase.split(*delimiters)

        for (i in result.indices) {
            val part = result[i]

            if (first || i != 0) {
                append(part[0].uppercaseChar())

                if (part.length > 1)
                    append(part.substring(1).lowercase())
            } else
                append(part.lowercase())

            if (i != result.lastIndex)
                append(separator)
        }
    }

private fun withDecimalsParsing(str: String, beginIdx: Int): Boolean {
    var decimalPoints = 0

    for (i in beginIdx until str.length) {
        val isDecimalPoint = str[i] == '.'
        if (isDecimalPoint) {
            ++decimalPoints
        }

        if (decimalPoints > 1) {
            return false
        }

        if (!isDecimalPoint && !Character.isDigit(str[i])) {
            return false
        }
    }

    return true
}