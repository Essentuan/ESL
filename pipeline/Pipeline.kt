package net.essentuan.esl.async.pipeline

import net.essentuan.esl.future.api.Future
import net.essentuan.esl.future.api.future
import net.essentuan.esl.coroutines.launch
import net.essentuan.esl.async.pipeline.extensions.pipe
import net.essentuan.esl.collector.maps.LinkedMapCollector
import net.essentuan.esl.iteration.Iterators
import net.essentuan.esl.iteration.extensions.iterable
import net.essentuan.esl.other.Result
import net.essentuan.esl.other.orElseThrow
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import java.util.Spliterator
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.streams.asSequence

interface Pipeline<T> : Publisher<T> {
    fun filter(predicate: Predicate<in T>): Pipeline<T>

    fun <R> map(mapper: Function<in T, out R>): Pipeline<R>

    fun <R> mapAsync(mapper: suspend (T) -> R): Pipeline<R> = await { future { mapper(it) } }

    fun <R> await(mapper: Function<T, out Future<R>>): Pipeline<R>

    fun <R> cast(cls: Class<R>): Pipeline<R>

    fun <R : Any> cast(cls: KClass<R>): Pipeline<R> {
        return cast(cls.java)
    }

    fun <R> cast(): Pipeline<R>

    fun concat(vararg streams: Sequence<T>): Pipeline<T>

    fun concat(vararg streams: Stream<T>): Pipeline<T>

    fun concat(vararg publishers: Publisher<T>): Pipeline<T>

    fun <R> flatMap(mapper: (T) -> Sequence<R>): Pipeline<R>

    fun distinct(extractor: (T) -> Any? = { it }): Pipeline<T>

    fun sorted(comparator: Comparator<in T>): Pipeline<T>

    fun sorted(): Pipeline<T>

    fun <C : Comparable<C>> sorted(extractor: Function<T, C>): Pipeline<T> {
        return sorted(Comparator.comparing(extractor))
    }

    fun peek(action: Consumer<in T>): Pipeline<T>

    fun limit(maxSize: Long): Pipeline<T>

    fun skip(n: Long): Pipeline<T>

    fun takeWhile(predicate: Predicate<in T>): Pipeline<T>

    fun dropWhile(predicate: Predicate<in T>): Pipeline<T>

    suspend fun reduce(identity: T, accumulator: BinaryOperator<T>): T

    suspend fun reduce(accumulator: BinaryOperator<T>): Result<T>

    suspend fun <U> reduce(identity: U, accumulator: BiFunction<U, in T, U>, combiner: BinaryOperator<U>): U

    suspend fun <R> collect(
        supplier: Supplier<R>,
        accumulator: BiConsumer<R, in T>,
        combiner: BiConsumer<R, R>
    ): R

    suspend fun <R, A> collect(collector: Collector<in T, A, R>): R

    suspend fun min(comparator: Comparator<in T>): Result<T>

    suspend fun max(comparator: Comparator<in T>): Result<T>

    suspend fun count(): Long

    suspend fun anyMatch(predicate: Predicate<in T>): Boolean

    suspend fun allMatch(predicate: Predicate<in T>): Boolean

    suspend fun noneMatch(predicate: Predicate<in T>): Boolean

    suspend fun findFirst(): Result<T>

    suspend fun first(): T = findFirst().orElseThrow { NoSuchElementException() }

    suspend fun findAny(): Result<T> {
        return findFirst()
    }

    suspend fun toMutableList(): MutableList<T> {
        return collect(Collectors.toList())
    }

    suspend fun toList(): List<T> {
        return collect(Collectors.toUnmodifiableList())
    }

    suspend fun toMutableSet(): MutableSet<T> {
        return collect(Collectors.toSet())
    }

    suspend fun toSet(): Set<T> {
        return collect(Collectors.toUnmodifiableSet())
    }

    suspend fun <K> toMutableMap(key: (T) -> K): MutableMap<K, T> {
        return toMutableMap(key) { it }
    }

    suspend fun <K> toMap(keyMapper: (T) -> K): Map<K, T> {
        return toMap(keyMapper) { it }
    }

    suspend fun <K, V> toMutableMap(keyMapper: (T) -> K, valueMapper: (T) -> V): MutableMap<K, V> {
        return collect(LinkedMapCollector(keyMapper, valueMapper))
    }

    suspend fun <K, V> toMap(keyMapper: (T) -> K, valueMapper: (T) -> V): Map<K, V> {
        return toMutableMap(keyMapper, valueMapper)
    }

    suspend fun forEach(action: Consumer<in T>)

    fun flux(): Flux<T>

    fun stream(): Stream<T>

    fun iterator(): Iterator<T>

    fun spliterator(): Spliterator<T>

    fun <U> gather(gatherer: Gatherer<T, U>): Pipeline<U>

    fun <U> gather(supplier: Supplier<Gatherer<T, U>>): Pipeline<U> {
        return gather(supplier.get())
    }

    fun <S : Subscriber<in T>> attach(subscriber: S): S

    override fun subscribe(s: Subscriber<in T>) {
        attach(s)
    }

    fun onClose(closeHandler: Runnable): Pipeline<T>

    fun close()

    companion object {
        fun <T> empty(): Pipeline<T> {
            return of(Iterators.empty())
        }

        fun <T> of(publisher: Publisher<T>): Pipeline<T> {
            return if (publisher is Pipeline<T>) publisher else RefPipeline(Gatherer.of(publisher))
        }

        @SafeVarargs
        fun <T> of(vararg values: T): Pipeline<T> {
            return of(Flux.fromArray(values))
        }

        @SafeVarargs
        fun <T> of(vararg promises: Future<T>): Pipeline<T> {
            return of(Flux.fromArray(promises)).await(Function.identity())
        }

        fun <T> of(iterable: Iterable<T>): Pipeline<T> {
            return of(Flux.fromIterable(iterable))
        }

        fun <T> of(iterator: Iterator<T>): Pipeline<T> {
            return of(iterator.iterable())
        }

        @JvmStatic
        fun <T> of(stream: Stream<T>): Pipeline<T> {
            return of(Flux.fromStream(stream))
        }

        fun <T> create(consumer: Consumer<Sink<T>>): Pipeline<T> {
            return of<T>(Flux.create((Consumer<FluxSink<T>> { consumer.accept(AsyncSink(it)) })))
        }

        fun <T> join(vararg sequences: Sequence<T>): Pipeline<T> {
            return of(*sequences).flatMap { it }
        }

        fun <T> join(vararg streams: Stream<T>): Pipeline<T> {
            return of(*streams).flatMap { it.asSequence() }
        }

        fun <T> join(vararg publishers: Publisher<T>): Pipeline<T> {
            return Flux.fromArray(publishers).flatMap { it }.pipe()
        }
    }
}

suspend inline fun <reified T> Pipeline<T>.toArray(): Array<T> = toList().toTypedArray<T>()

fun <T> pipeline(block: suspend Sink<T>.() -> Unit): Pipeline<T> = Pipeline.create {
    launch {
        try {
            block(it)

            it.complete()
        } catch (e: Throwable) {
            it.error(e)
        }
    }
}