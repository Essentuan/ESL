package net.essentuan.esl.color

import net.essentuan.esl.comparing.equals
import net.essentuan.esl.other.clamp
import net.essentuan.esl.other.repr
import java.util.Objects
import kotlin.math.max
import kotlin.math.min

internal data class ColorImpl(
    override val red: Int,
    override val green: Int,
    override val blue: Int,
    override val alpha: Int
) : Color {
    override fun brighten(factor: Float): Color {
        val i = (1.0 / (1.0 - factor)).toInt()

        if (red == 0 && green == 0 && blue == 0)
            return with(red = i, green = i, blue = i)

        return with(
            red = min(max(red, i) / factor, 255.0f).toInt(),
            green = min(max(green, i) / factor, 255.0f).toInt(),
            blue = min(max(blue, i) / factor, 255.0f).toInt(),
        )
    }

    override fun darken(factor: Float): Color = with(
        red = max(red / factor, 0f).toInt(),
        green = max(green / factor, 0f).toInt(),
        blue = max(blue / factor, 0f).toInt()
    )

    override fun with(red: Int, green: Int, blue: Int, alpha: Int): Color = ColorImpl(
        red.clamp(0, 255),
        green.clamp(0, 255),
        blue.clamp(0, 255),
        alpha.clamp(0, 255)
    )

    override fun with(red: Float, green: Float, blue: Float, alpha: Float): Color = with(
        (red * 255f).toInt(),
        (green * 255f).toInt(),
        (blue * 255f).toInt(),
        (alpha * 255f).toInt()
    )

    override fun asInt(): Int = (alpha shl 24) or (red shl 16) or (green shl 8) or blue

    override fun asOpaque(): Int = (red shl 16) or (green shl 8) or blue

    override fun asFloatArray(): FloatArray = floatArrayOf(red / 255f, green / 255f, blue / 255f)

    override fun asHex(): String = "#${asOpaque().toString(16)}${if (alpha == 255) "" else alpha.toString(16)}"

    override fun toString(): String = repr {
        prefix(Color::class)

        + Color::red
        + Color::green
        + Color::blue
        + Color::alpha
    }

    override fun hashCode(): Int = Objects.hash(red, green, blue, alpha)

    override fun equals(other: Any?): Boolean = equals<Color>(other) {
        + Color::red
        + Color::green
        + Color::blue
        + Color::alpha
    }
}