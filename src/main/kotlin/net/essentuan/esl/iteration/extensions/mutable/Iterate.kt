package net.essentuan.esl.iteration.extensions.mutable

import net.essentuan.esl.iteration.extensions.iterate

inline infix fun <T> MutableIterable<T>.iterate(
    crossinline block: MutableIterator<T>.(T) -> Unit
) = this.iterator() iterate block