package net.essentuan.esl.iteration.extensions

fun <T> Iterable<T>.append(vararg elements: T): MutableList<T> {
    val list = this.toMutableList()
    list.addAll(elements)

    return list
}

fun <T> Iterable<T>.appendAll(vararg elements: Iterable<T>): MutableList<T> {
    val list = this.toMutableList()
    list.addAll(elements.flatMap { e ->
        object : Iterable<T> {
            override fun iterator(): Iterator<T> {
                return e.iterator()
            }
        }
    })

    return list
}

fun <T> Iterable<T>.prepend(vararg elements: T): MutableList<T> {
    val list = mutableListOf(*elements)
    list.addAll(this)

    return list
}

fun <T> Iterable<T>.prependAll(vararg elements: Iterable<T>): MutableList<T> {
    val list: MutableList<T> = elements.flatMap { e ->
        object : Iterable<T> {
            override fun iterator(): Iterator<T> {
                return e.iterator()
            }
        }
    }.toMutableList()
    list.addAll(this)

    return list
}

inline infix fun <T> Iterable<T>.iterate(
    block: Iterator<T>.(T) -> Unit
) = this.iterator() iterate block