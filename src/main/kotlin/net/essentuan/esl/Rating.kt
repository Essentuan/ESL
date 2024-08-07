package net.essentuan.esl

enum class Rating {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    CRITICAL;

    fun next(): Rating {
        if (this == LOWEST)
            throw NoSuchElementException()

        return entries[ordinal - 1]
    }
}