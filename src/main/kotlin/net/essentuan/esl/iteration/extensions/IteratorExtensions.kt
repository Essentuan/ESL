package net.essentuan.esl.iteration.extensions

import net.essentuan.esl.iteration.BreakException
import net.essentuan.esl.iteration.Iterators
import net.essentuan.esl.iteration.iterators.FilterIter
import net.essentuan.esl.iteration.iterators.MappedIter
import net.essentuan.esl.other.unsupported
import java.util.Spliterator
import java.util.Spliterators
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Stream
import java.util.stream.StreamSupport

fun <T> Iterator<T>.concat(vararg iters: Iterator<T>): Iterator<T> {
    return Iterators.concat(listOf(this).append(*iters))
}

fun <T, U> Iterator<T>.concat(vararg iters: Iterator<U>, mapper: Function<U, T>): Iterator<T> {
    return Iterators.join(
        this,
        Iterators.join(*iters).map(mapper)
    )
}

fun <T, U> Iterator<T>.flatMap(mapper: Function<T, Iterator<U>>): Iterator<U> {
    return Iterators.concat(map(mapper))
}

fun <T, U> Iterator<T>.map(mapper: Function<T, U>): Iterator<U> {
    return MappedIter.Immutable(this, mapper)
}

fun <T, U> Iterator<T>.cast(cls: Class<U>): Iterator<U> {
    return map(cls::cast)
}

@Suppress("UNCHECKED_CAST")
fun <T, U> Iterator<T>.cast(): Iterator<U> {
    return map { it as U }
}

fun <T> Iterator<T>.filter(predicate: Predicate<T>): Iterator<T> {
    return FilterIter(this, predicate)
}

fun <T> Iterator<T>.distinct(mapper: Function<T, Any?> = Function { it }): Iterator<T> {
    val seen = HashSet<Any?>()

    return filter { e -> seen.add(mapper.apply(e)) }
}

fun <T> Iterator<T>.append(vararg items: T): Iterator<T> {
    return Iterators.join(this, Iterators.of(*items))
}

fun <T> Iterator<T>.append(items: Iterable<T>): Iterator<T> {
    return Iterators.join(this, items.iterator())
}

fun <T> Iterator<T>.append(items: Iterator<T>): Iterator<T> {
    return Iterators.join(this, items)
}

fun <T> Iterator<T>.prepend(vararg items: T): Iterator<T> {
    return Iterators.join(Iterators.of(*items), this)
}

fun <T> Iterator<T>.prepend(items: Iterable<T>): Iterator<T> {
    return Iterators.join(items.iterator(), this)
}

fun <T> Iterator<T>.prepend(items: Iterator<T>): Iterator<T> {
    return Iterators.join(items, this)
}

fun <T> Iterator<T>.immutable(): MutableIterator<T> {
    return object : MutableIterator<T> {
        override fun hasNext(): Boolean {
            return this@immutable.hasNext()
        }

        override fun next(): T {
            return this@immutable.next()
        }

        override fun remove() {
            unsupported()
        }
    }
}


fun <T> Iterator<T>.spliterator(): Spliterator<T> {
    return Spliterators.spliteratorUnknownSize(this, 0)
}

fun <T> Iterator<T>.stream(): Stream<T> {
    return StreamSupport.stream(spliterator(), false);
}

fun <T> Iterator<T>.iterable(): Iterable<T> = Iterable { this }

inline infix fun <T, ITER: Iterator<T>> ITER.iterate(
    block: ITER.(T) -> Unit
) {
    try {
        while (hasNext())
            block(next())
    } catch (ex: Throwable) {
        if (ex is BreakException)
            return
        else
            throw ex
    }
}
