package net.essentuan.esl.iteration

class BreakException : RuntimeException()

fun Iterator<*>.`break`(): Nothing
    = throw BreakException()