package net.essentuan.esl.image

import net.essentuan.esl.color.Color
import net.essentuan.esl.iteration.extensions.iterate
import java.awt.image.BufferedImage

typealias Image = BufferedImage

operator fun Image.get(x: Int, y: Int): Pixel = PixelImpl(x, y, Color(this.getRGB(x, y)))

operator fun Image.set(x: Int, y: Int, color: Color) = this.setRGB(x, y, color.asInt())

operator fun Image.set(x: Int, y: Int, color: Int) = this.setRGB(x, y, color)

fun Image.iterator(step: Int): Iterator<Pixel> = iterator scope@{
    for (y in 0 until height step step)
        for (x in 0 until width step step)
            yield(this@iterator[x, y])
}

operator fun Image.iterator(): Iterator<Pixel> = iterator(1)

inline infix fun Image.iterate(crossinline block: Iterator<Pixel>.(Pixel) -> Unit) =
    iterate(1, block)

inline fun Image.iterate(step: Int, crossinline block: Iterator<Pixel>.(Pixel) -> Unit) =
    this.iterator(step).iterate(block)

internal class PixelImpl(
    override val x: Int,
    override val y: Int,
    val color: Color
) : Pixel, Color by color