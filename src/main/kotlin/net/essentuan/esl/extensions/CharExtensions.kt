package net.essentuan.esl.extensions


/**
 * Checks if two chars are equal; ignores case
 */
infix fun Char?.ceq(other: Char?) = this?.lowercaseChar() == other?.lowercaseChar()