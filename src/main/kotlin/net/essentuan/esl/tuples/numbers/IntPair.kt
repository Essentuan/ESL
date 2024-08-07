package net.essentuan.esl.tuples.numbers

@JvmInline
value class IntPair(
    private val data: Long
) {
    constructor(
        first: Int,
        second: Int
    ) : this((first.toLong() shl 32) or (second.toLong() and MASK))

    operator fun component1(): Int = first

    operator fun component2(): Int = second

    val first: Int
        get() = (data ushr 32).toInt()

    val second: Int
        get() = (data and MASK).toInt()

    operator fun plus(other: IntPair) = IntPair(
        first + other.first,
        second + other.second
    )

    operator fun plus(other: Int) = IntPair(
        first + other,
        second + other
    )

    operator fun plus(other: Long) = IntPair(
        first + other.toInt(),
        second + other.toInt()
    )

    operator fun plus(other: Float) = IntPair(
        first + other.toInt(),
        second + other.toInt()
    )

    operator fun plus(other: Double) = IntPair(
        first + other.toInt(),
        second + other.toInt()
    )

    operator fun minus(other: IntPair) = IntPair(
        first - other.first,
        second - other.second
    )

    operator fun minus(other: Int) = IntPair(
        first - other,
        second - other
    )

    operator fun minus(other: Long) = IntPair(
        first - other.toInt(),
        second - other.toInt()
    )

    operator fun minus(other: Float) = IntPair(
        first - other.toInt(),
        second - other.toInt()
    )

    operator fun minus(other: Double) = IntPair(
        first - other.toInt(),
        second - other.toInt()
    )

    operator fun times(other: IntPair) = IntPair(
        first * other.first,
        second * other.second
    )

    operator fun times(other: Int) = IntPair(
        first * other,
        second * other
    )

    operator fun times(other: Long) = IntPair(
        first * other.toInt(),
        second * other.toInt()
    )

    operator fun times(other: Float) = IntPair(
        first * other.toInt(),
        second * other.toInt()
    )

    operator fun times(other: Double) = IntPair(
        first * other.toInt(),
        second * other.toInt()
    )

    operator fun div(other: IntPair) = IntPair(
        first / other.first,
        second / other.second
    )

    operator fun div(other: Int) = IntPair(
        first / other,
        second / other
    )

    operator fun div(other: Long) = IntPair(
        first / other.toInt(),
        second / other.toInt()
    )

    operator fun div(other: Float) = IntPair(
        first / other.toInt(),
        second / other.toInt()
    )

    operator fun div(other: Double) = IntPair(
        first / other.toInt(),
        second / other.toInt()
    )

    operator fun rem(other: IntPair) = IntPair(
        first % other.first,
        second % other.second
    )

    operator fun rem(other: Int) = IntPair(
        first % other,
        second % other
    )

    operator fun rem(other: Long) = IntPair(
        first % other.toInt(),
        second % other.toInt()
    )

    operator fun rem(other: Float) = IntPair(
        first % other.toInt(),
        second % other.toInt()
    )

    operator fun rem(other: Double) = IntPair(
        first % other.toInt(),
        second % other.toInt()
    )

    fun copy(first: Int = this.first, second: Int = this.second) =
        IntPair(first, second)

    override fun toString(): String {
        return "IntPair[first=$first, second=$second]"
    }

    companion object {
        val ZERO = IntPair(0, 0)
    }
}