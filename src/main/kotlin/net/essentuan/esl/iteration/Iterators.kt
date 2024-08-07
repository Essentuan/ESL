package net.essentuan.esl.iteration

import com.google.common.collect.Iterators
import net.essentuan.esl.iteration.iterators.ArrayIter

typealias Guava = Iterators

class Iterators {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): Iterator<T> {
            return SimpleIterator.EMPTY as Iterator<T>
        }

        fun <T> of(vararg items: T): Iterator<T> {
            return ArrayIter(items)
        }

        fun <T> join(vararg iters: Iterator<T>): MutableIterator<T> {
            return concat(of(*iters))
        }

        fun <T> concat(iters: Iterable<Iterator<T>>): MutableIterator<T> {
            return concat(iters.iterator())
        }

        fun <T> concat(iters: Iterator<Iterator<T>>): MutableIterator<T> = Guava.concat(iters)
    }
}