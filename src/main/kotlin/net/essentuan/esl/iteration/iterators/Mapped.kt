package net.essentuan.esl.iteration.iterators

import java.util.function.Function

internal abstract class MappedIter<I, O, Iter: Iterator<I>> private constructor(
    val iter: Iter,
    val mapper: Function<I, O>
) : Iterator<O> {
    override fun hasNext(): Boolean {
        return iter.hasNext()
    }

    override fun next(): O {
        return mapper.apply(iter.next())
    }

    class Mutable<I, O>(iter: MutableIterator<I>, mapper: Function<I, O>) :
        MappedIter<I, O, MutableIterator<I>>(iter, mapper), MutableIterator<O> {
        override fun remove() {
            iter.remove()
        }
    }

    class Immutable<I, O>(iter: Iterator<I>, mapper: Function<I, O>) :
        MappedIter<I, O, Iterator<I>>(iter, mapper), Iterator<O>
}