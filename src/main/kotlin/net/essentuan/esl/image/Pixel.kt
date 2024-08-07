package net.essentuan.esl.image

import net.essentuan.esl.color.Color

interface Pixel : Color {
    val x: Int
    val y: Int
}