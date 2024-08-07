package net.essentuan.esl.string.reader

internal class StringReaderImpl(override val string: String, override var cursor: Int) : StringReader, CharSequence by string {
    override fun read(): Char = string[cursor++]

    override fun readInt(): Int = readWhile(predicate = Companion::isAllowedNumber).toInt()

    override fun readLong(): Long = readWhile(predicate = Companion::isAllowedNumber).toLong()

    override fun readFloat(): Float = readWhile(predicate = Companion::isAllowedNumber).toFloat()

    override fun readDouble(): Double = readWhile(predicate = Companion::isAllowedNumber).toDouble()

    override fun readBoolean(): Boolean {
        return when (val str = readString()) {
            "true" -> true
            "false" -> false
            else -> error("Expected boolean (true/false) but found ${str}!")
        }
    }

    override fun readString(): String {
        if (!canRead())
            return ""

        val next = peek()

        if (!isQuotedStringStart(next))
            return readWhile(predicate = Companion::isAllowedInRegularString)

        skip()

        return readUntil(next).also {
            skip()
        }
    }

    override fun readWhile(map: (Char) -> Char, skip: (Char) -> Boolean, predicate: (Char) -> Boolean): String = readUntil(map) { !predicate(it) }

    override fun readUntil(map: (Char) -> Char, skip: (Char) -> Boolean, predicate: (Char) -> Boolean): String {
        if (!canRead())
            return ""

        val result = StringBuilder()

        var escaped = false

        while (canRead()) {
            val c = peek()

            when {
                escaped -> {
                    result.append(map(c))
                    escaped = false
                }

                c == SYNTAX_ESCAPE -> {
                    escaped = true
                }

                skip(c) -> Unit

                predicate(c) -> return result.toString()
                else -> result.append(map(c))
            }

            skip()
        }

        return result.toString()
    }

    override fun skip() {
        cursor++
    }

    override fun skipWhile(predicate: (Char) -> Boolean) {
        while (canRead() && predicate(peek()))
            skip()
    }

    override fun skipWhitespace() = skipWhile(Character::isWhitespace)

    override fun expect(c: Char) {
       check(c == peek()) { error("Expected $c but found ${peek()}!") }

        skip()
    }

    private companion object {
        private const val SYNTAX_ESCAPE = '\\'
        private const val SYNTAX_DOUBLE_QUOTE = '"'
        private const val SYNTAX_SINGLE_QUOTE = '\''

        fun isAllowedNumber(c: Char): Boolean {
            return c in '0'..'9' || c == '.' || c == '-'
        }

        fun isQuotedStringStart(c: Char): Boolean {
            return c == SYNTAX_DOUBLE_QUOTE || c == SYNTAX_SINGLE_QUOTE
        }

        fun isAllowedInRegularString(c: Char): Boolean {
            return c in '0'..'9' || c in 'A'..'Z' || c in 'a'..'z' || c == '_' || c == '-' || c == '.' || c == '+'
        }
    }
}