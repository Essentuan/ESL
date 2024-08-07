package net.essentuan.esl.rx

import net.essentuan.esl.rx.producers.IteratorProducer
import net.essentuan.esl.rx.publishers.ConcatenatedPublisher
import net.essentuan.esl.rx.publishers.MergedPublisher
import org.reactivestreams.Publisher
import java.util.stream.Stream

object Publishers {
    fun <T> merge(vararg publishers: Publisher<T>): Publisher<T> = MergedPublisher(*publishers)

    fun <T> concat(vararg publisher: Publisher<T>): Publisher<T> = ConcatenatedPublisher(*publisher)
}

fun <T> Iterable<T>.publish(): Publisher<T> = Publisher { IteratorProducer(it, this.iterator()).subscribe() }

fun <T> Array<T>.publish(): Publisher<T> = Publisher { IteratorProducer(it, this.iterator()).subscribe() }

fun <T> Sequence<T>.publish(): Publisher<T> = Publisher { IteratorProducer(it, this.iterator()).subscribe() }

fun <T> Stream<T>.publish(): Publisher<T> = Publisher { IteratorProducer(it, this.iterator()).subscribe() }

fun <T> Iterator<T>.publish(): Publisher<T> = Publisher { IteratorProducer(it, this).subscribe() }

operator fun <T> Publisher<T>.plus(publisher: Publisher<T>) =
    Publishers.concat(this, publisher)

operator fun <T> Publisher<T>.plus(iterable: Iterable<T>) =
    Publishers.concat(this, iterable.publish())

operator fun <T> Iterable<T>.plus(publisher: Publisher<T>) =
    Publishers.concat(publish(), publisher)

operator fun <T> Publisher<T>.plus(array: Array<T>) =
    Publishers.concat(this, array.publish())

operator fun <T> Array<T>.plus(publisher: Publisher<T>) =
    Publishers.concat(publish(), publisher)

operator fun <T> Publisher<T>.plus(sequence: Sequence<T>) =
    Publishers.concat(this, sequence.publish())

operator fun <T> Sequence<T>.plus(publisher: Publisher<T>) =
    Publishers.concat(publish(), publisher)

operator fun <T> Publisher<T>.plus(stream: Stream<T>) =
    Publishers.concat(this, stream.publish())

operator fun <T> Stream<T>.plus(publisher: Publisher<T>) =
    Publishers.concat(publish(), publisher)

operator fun <T> Publisher<T>.plus(iterator: Iterator<T>) =
    Publishers.concat(this, iterator.publish())

operator fun <T> Iterator<T>.plus(publisher: Publisher<T>) =
    Publishers.concat(publish(), publisher)

inline fun <T> publisher(crossinline block: suspend PublisherScope<T>.() -> Unit): Publisher<T> =
    Publisher {
        object : Generator<T>(it) {
            override suspend fun generate() = block()
        }.subscribe()
    }