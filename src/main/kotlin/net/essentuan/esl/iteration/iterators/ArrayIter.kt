package net.essentuan.esl.iteration.iterators

import net.essentuan.esl.iteration.SimpleIterator

internal class ArrayIter<T>(val array: Array<T>) : SimpleIterator<T>() {
    var cursor = 0;

    override fun compute(): T {
        return array[cursor++]
    }

    override fun hasNext(): Boolean {
        return array.size > cursor
    }
}