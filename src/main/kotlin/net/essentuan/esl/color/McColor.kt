package net.essentuan.esl.color

import net.essentuan.esl.other.Printable
import net.essentuan.esl.string.extensions.camelCase

private const val IMPLICIT: String = ""

/**
 * Colors from Minecraft
 */
enum class McColor(
    val color: Color,
    displayName: String = IMPLICIT,
    id: String = IMPLICIT
): Color by color, Printable {
    BLACK(0x000000),
    DARK_BLUE(0x0000AA),
    DARK_GREEN(0x00AA00),
    DARK_AQUA(0x00AAAA),
    DARK_RED(0xAA0000),
    DARK_PURPLE(0xAA00AA),
    GOLD(0xFFAA00),
    GRAY(0xAAAAAA),
    DARK_GRAY(0x555555),
    BLUE(0x5555FF),
    GREEN(0x55FF55),
    AQUA(0x55FFFF),
    RED(0xFF5555),
    LIGHT_PURPLE(0xFF55FF),
    YELLOW(0xFFFF55),
    WHITE(0xFFFFFF);

    constructor(num: Int, displayName: String = IMPLICIT, id: String = IMPLICIT) : this(num.toColor(255), displayName, id)

    val displayName: String = if (displayName === IMPLICIT) name.camelCase(first = true, separator = " ") else displayName
    val id: String = if (id === IMPLICIT) name.camelCase(first = true, separator = " ") else id

    val background: Color = Color(
        red / 4,
        green / 4,
        blue / 4
    )

    override fun print(): String = displayName
}