package net.essentuan.esl.tuples.numbers

internal const val MASK: Long = 0xFFFFFFFF

@JvmInline
value class FloatPair(
    private val data: Long
) {
    constructor(
        first: Float,
        second: Float
    ) : this((first.toRawBits().toLong() shl 32) or (second.toRawBits().toLong() and MASK))

    operator fun component1(): Float = first

    operator fun component2(): Float = second

    val first: Float
        get() = Float.fromBits((data ushr 32).toInt())

    val second: Float
        get() = Float.fromBits((data and MASK).toInt())

    operator fun plus(other: FloatPair) = FloatPair(
        first + other.first,
        second + other.second
    )

    operator fun plus(other: Int) = FloatPair(
        first + other.toFloat(),
        second + other.toFloat()
    )

    operator fun plus(other: Long) = FloatPair(
        first + other.toFloat(),
        second + other.toFloat()
    )

    operator fun plus(other: Float) = FloatPair(
        first + other,
        second + other
    )

    operator fun plus(other: Double) = FloatPair(
        first + other.toFloat(),
        second + other.toFloat()
    )

    operator fun minus(other: FloatPair) = FloatPair(
        first - other.first,
        second - other.second
    )

    operator fun minus(other: Int) = FloatPair(
        first - other.toFloat(),
        second - other.toFloat()
    )

    operator fun minus(other: Long) = FloatPair(
        first - other.toFloat(),
        second - other.toFloat()
    )

    operator fun minus(other: Float) = FloatPair(
        first - other,
        second - other
    )

    operator fun minus(other: Double) = FloatPair(
        first - other.toFloat(),
        second - other.toFloat()
    )

    operator fun times(other: FloatPair) = FloatPair(
        first * other.first,
        second * other.second
    )

    operator fun times(other: Int) = FloatPair(
        first * other.toFloat(),
        second * other.toFloat()
    )

    operator fun times(other: Long) = FloatPair(
        first * other.toFloat(),
        second * other.toFloat()
    )

    operator fun times(other: Float) = FloatPair(
        first * other,
        second * other
    )

    operator fun times(other: Double) = FloatPair(
        first * other.toFloat(),
        second * other.toFloat()
    )

    operator fun div(other: FloatPair) = FloatPair(
        first / other.first,
        second / other.second
    )

    operator fun div(other: Int) = FloatPair(
        first / other.toFloat(),
        second / other.toFloat()
    )

    operator fun div(other: Long) = FloatPair(
        first / other.toFloat(),
        second / other.toFloat()
    )

    operator fun div(other: Float) = FloatPair(
        first / other,
        second / other
    )

    operator fun div(other: Double) = FloatPair(
        first / other.toFloat(),
        second / other.toFloat()
    )

    operator fun rem(other: FloatPair) = FloatPair(
        first % other.first,
        second % other.second
    )

    operator fun rem(other: Int) = FloatPair(
        first % other.toFloat(),
        second % other.toFloat()
    )

    operator fun rem(other: Long) = FloatPair(
        first % other.toFloat(),
        second % other.toFloat()
    )

    operator fun rem(other: Float) = FloatPair(
        first % other,
        second % other
    )

    operator fun rem(other: Double) = FloatPair(
        first % other.toFloat(),
        second % other.toFloat()
    )

    fun copy(first: Float = this.first, second: Float = this.second) =
        FloatPair(first, second)

    override fun toString(): String {
        return "FloatPair[first=$first, second=$second]"
    }

    companion object {
        val ZERO = FloatPair(0f, 0f)
    }
}