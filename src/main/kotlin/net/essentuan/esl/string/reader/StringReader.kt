package net.essentuan.esl.string.reader

interface StringReader : CharSequence {
    val string: String
    val cursor: Int

    val remaining: Int
        get() = string.length - cursor

    fun canRead(length: Int = 1) = cursor + length <= string.length

    fun peek(offset: Int = 0): Char = string[cursor + offset]

    fun peekStr(length: Int = 1): String = string.substring(cursor, cursor + length)

    fun read(): Char

    fun readInt(): Int

    fun readLong(): Long

    fun readFloat(): Float

    fun readDouble(): Double

    fun readBoolean(): Boolean

    fun readString(): String

    fun readWhile(map: (Char) -> Char = { it }, skip: (Char) -> Boolean = { false }, predicate: (Char) -> Boolean): String

    fun readUntil(map: (Char) -> Char = { it }, skip: (Char) -> Boolean = { false }, predicate: (Char) -> Boolean): String

    fun readUntil(terminator: Char, map: (Char) -> Char = { it }, skip: (Char) -> Boolean = { false }): String = readUntil(map = map, skip = skip) { c -> c == terminator }

    fun skip()

    fun skipWhile(predicate: (Char) -> Boolean)

    fun skipWhitespace()

    fun expect(c: Char)
}

fun String.consume(cursor: Int = 0): StringReader = StringReaderImpl(this, cursor)

fun String.consume(cursor: Int = 0, consumer: StringReader.() -> Unit) = consume(cursor).run {
    while (canRead())
        consumer(this)
}