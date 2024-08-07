package net.essentuan.esl.color

import net.essentuan.esl.color.Color.Companion.invoke
import net.essentuan.esl.encoding.AbstractEncoder
import net.essentuan.esl.other.clamp
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

typealias AwtColor = java.awt.Color


/**
* See [ColorConverter](https://github.com/accord-net/java/blob/master/Catalano.Image/src/Catalano/Imaging/Tools/ColorConverter.java)
 */
interface Color {
    val red: Int
    val green: Int
    val blue: Int
    val alpha: Int

    fun brighten(factor: Float = 0.7f): Color

    fun darken(factor: Float = 0.7f): Color

    fun with(
        red: Int = this.red,
        green: Int = this.green,
        blue: Int = this.blue,
        alpha: Int = this.alpha
    ): Color

    fun with(
        red: Float = this.red / 255f,
        green: Float = this.green / 255f,
        blue: Float = this.blue / 255f,
        alpha: Float = this.alpha / 255f
    ): Color

    fun asInt(): Int

    fun asOpaque(): Int

    fun asFloatArray(): FloatArray

    fun asHex(): String

    fun asAwt(): AwtColor = AwtColor(asInt(), true)

    fun asHSV(): Triple<Float, Float, Float> {
        val r = red / 255f
        val g = green / 255f
        val b = blue / 255f

        val max = max(r.toDouble(), max(g.toDouble(), b.toDouble())).toFloat()
        val min = min(r.toDouble(), min(g.toDouble(), b.toDouble())).toFloat()
        val delta = max - min

        return Triple(
            when (max) {
                min -> 0f
                r -> ((g - b) / delta) * 60f
                g -> ((b - r) / delta + 2f) * 60f
                b -> ((r - g) / delta + 4f) * 60f
                else -> throw IllegalArgumentException()
            },
            if (delta == 0f) 0f else delta / max,
            max
        )
    }

    fun asHsl(): Triple<Float, Float, Float> {
        val r = red / 255f
        val g = green / 255f
        val b = blue / 255f

        val max = max(r.toDouble(), max(r.toDouble(), b.toDouble())).toFloat()
        val min = min(r.toDouble(), min(r.toDouble(), b.toDouble())).toFloat()
        val delta = max - min

        val h: Float
        val s: Float
        val l = (max + min) / 2

        if (delta == 0f) {
            h = 0f
            s = 0.0f
        } else {
            s = if ((l <= 0.5)) (delta / (max + min)) else (delta / (2 - max - min))

            var hue = if (r == max) {
                (g - b) / 6 / delta
            } else if (g == max) {
                1.0f / 3 + ((b - r) / 6) / delta
            } else {
                2.0f / 3 + ((r - g) / 6) / delta
            }

            if (hue < 0) hue += 1f
            if (hue > 1) hue -= 1f

            h = (hue * 360).toInt().toFloat()
        }

        return Triple(h, s, l)
    }

    companion object {
        @JvmName("fromRgb")
        operator fun invoke(red: Int, green: Int, blue: Int, alpha: Int = 255): Color =
            ColorImpl(
                red.clamp(0, 255),
                green.clamp(0, 255),
                blue.clamp(0, 255),
                alpha.clamp(0, 255)
            )

        @JvmName("fromRgb")
        operator fun invoke(red: Float, green: Float, blue: Float, alpha: Float = 1f, factor: Float = 255f): Color =
            invoke(
                (red * factor).toInt(),
                (green * factor).toInt(),
                (blue * factor).toInt(),
                (alpha * factor).toInt()
            )

        @JvmName("fromHsv")
        operator fun invoke(hue: Float, saturation: Float, value: Float, alpha: Int = 255): Color {
            val hi = floor(hue / 60.0).toFloat() % 6
            val f = ((hue / 60.0) - floor(hue / 60.0)).toFloat()
            val p = (value * (1.0 - saturation)).toFloat()
            val q = (value * (1.0 - (f * saturation))).toFloat()
            val t = (value * (1.0 - ((1.0 - f) * saturation))).toFloat()

            return when (hi) {
                0f -> Color(
                    (value * 255).toInt(),
                    (t * 255).toInt(),
                    (p * 255).toInt(),
                    alpha
                )
                1f -> Color(
                    (q * 255).toInt(),
                    (value * 255).toInt(),
                    (p * 255).toInt(),
                    alpha
                )
                2f -> Color(
                    (p * 255).toInt(),
                    (value * 255).toInt(),
                    (t * 255).toInt(),
                    alpha
                )
                3f -> Color(
                    (p * 255).toInt(),
                    (value * 255).toInt(),
                    (q * 255).toInt(),
                    alpha
                )
                4f -> Color(
                    (t * 255).toInt(),
                    (value * 255).toInt(),
                    (p * 255).toInt(),
                    alpha
                )
                5f -> Color(
                    (value * 255).toInt(),
                    (p * 255).toInt(),
                    (q * 255).toInt(),
                    alpha
                )
                else -> throw IllegalArgumentException()
            }
        }

        @JvmName("fromHsl")
        operator fun invoke(hue: Float, saturation: Float, luminance: Float, alpha: Float = 1f): Color {
            return if (saturation == 0f)
                Color(
                    red = luminance,
                    green = luminance,
                    blue = luminance,
                    alpha
                )
            else {
                val v1: Float
                val h = hue / 360

                val v2 =
                    if ((luminance < 0.5)) (luminance * (1 + saturation)) else ((luminance + saturation) - (luminance * saturation))
                v1 = 2 * luminance - v2

                Color(
                    red = hueToRgb(v1, v2, h + (1.0f / 3)),
                    green = hueToRgb(v1, v2, h + (1.0f / 3)),
                    blue = hueToRgb(v1, v2, h - (1.0f / 3)),
                    alpha
                )
            }
        }

        private fun hueToRgb(v1: Float, v2: Float, vH: Float): Float {
            var vH = vH
            if (vH < 0) vH += 1f
            if (vH > 1) vH -= 1f
            if ((6 * vH) < 1) return (v1 + (v2 - v1) * 6 * vH)
            if ((2 * vH) < 1) return v2
            if ((3 * vH) < 2) return (v1 + (v2 - v1) * ((2.0f / 3) - vH) * 6)
            return v1
        }


        @JvmName("fromInt")
        operator fun invoke(num: Int, alpha: Boolean = false): Color {
            val a = (num shr 24) and 255

            return invoke(
                (num shr 16) and 255,
                (num shr 8) and 255,
                num and 255,
                if (a != 0 || alpha) a else 255
            )
        }

        @JvmName("fromInt")
        operator fun invoke(num: Int, alpha: Int): Color = invoke(
            (num shr 16) and 255,
            (num shr 8) and 255,
            num and 255,
            alpha.clamp(0, 255)
        )

        @JvmName("fromString")
        operator fun invoke(str: String): Color {
            require(str.isNotEmpty()) {
                "String cannot be empty!"
            }

            val start = if(str[0] == '#') 1 else 0
            val end = 6 + start

            return invoke(
                str.substring(start, end).toInt(16),
                if (str.length > end) str.substring(end, min(str.length, 8 + start)).toInt(16) else 255
            )
        }
    }
}

fun Number.toColor(alpha: Boolean = false): Color = invoke(toInt(), alpha = alpha)

fun Number.toColor(alpha: Int): Color = invoke(toInt(), alpha = alpha)

class Writer : AbstractEncoder<Color, Int>() {
    override fun encode(obj: Color, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Int = obj.asInt()

    override fun decode(obj: Int, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Color = invoke(obj)

    override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Color = invoke(string)

    override fun toString(obj: Color, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String = obj.asHex()
}