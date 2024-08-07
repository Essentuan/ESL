package net.essentuan.esl.iteration.iterators

import java.util.function.Predicate

private val END_MARKER = Any()

internal class FilterIter<T>(private val iter: Iterator<T>, private val predicate: Predicate<T>) : Iterator<T> {
    private var next: Any? = null

    private fun peek(): Any {
        when {
            next === END_MARKER -> return END_MARKER
            next != null -> return next!!
            !iter.hasNext() -> {
                finish()

                return END_MARKER
            }
        }

        while (iter.hasNext() && next == null) {
            val value = iter.next()
            if (predicate.test(value)) this.next = value
        }

        if (next == null) {
            finish()

            return END_MARKER
        }

        return next!!
    }

    private fun finish() {
        next = END_MARKER
    }

    override fun hasNext(): Boolean {
        return peek() != END_MARKER
    }

    @Suppress("UNCHECKED_CAST")
    override fun next(): T {
        val next = peek()

        if (next == END_MARKER)
            throw NoSuchElementException()

        this.next = null
        return next as T
    }
}
