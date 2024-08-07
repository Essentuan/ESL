@file:OptIn(ExperimentalTypeInference::class)

package net.essentuan.esl.rx

import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import kotlinx.coroutines.suspendCancellableCoroutine
import net.essentuan.esl.Result
import net.essentuan.esl.collections.builders.MapBuilder
import net.essentuan.esl.collections.builders.MultiMapBuilder
import net.essentuan.esl.get
import net.essentuan.esl.isEmpty
import net.essentuan.esl.other.unsupported
import net.essentuan.esl.result
import org.reactivestreams.Publisher
import java.math.BigDecimal
import java.math.BigInteger
import java.util.function.BinaryOperator
import kotlin.experimental.ExperimentalTypeInference

suspend fun <IN, OUT> Publisher<IN>.subscribe(
    n: Long = Int.MAX_VALUE.toLong(),
    complete: ContinuationSubscriber<IN, OUT>.() -> Unit,
    next: ContinuationSubscriber<IN, OUT>.(IN) -> Unit
): OUT = suspendCancellableCoroutine {
    subscribe(object : ContinuationSubscriber<IN, OUT>(it, n) {
        @Synchronized
        override fun onComplete() {
            if (open)
                this.complete()
        }

        @Synchronized
        override fun onNext(value: IN) {
            if (open)
                this.next(value)
        }
    })
}

suspend fun <IN, OUT> Publisher<IN>.reduce(
    identity: OUT,
    accumulator: OUT.(IN) -> OUT
): OUT = suspendCancellableCoroutine {
    subscribe(object : ContinuationSubscriber<IN, OUT>(it) {
        var current: OUT = identity

        @Synchronized
        override fun onComplete() {
            if (open)
                yield(current)
        }

        @Synchronized
        override fun onNext(value: IN) {
            if (open)
                current = current.accumulator(value)
        }
    })
}

suspend fun <T> Publisher<T>.accumulate(accumulator: BinaryOperator<T>): Result<T> =
    reduce(Result.empty()) {
        if (isEmpty()) it.result() else accumulator.apply(get(), it).result()
    }

suspend fun <IN, OUT> Publisher<IN>.collect(
    container: OUT,
    accumulator: OUT.(IN) -> Unit
): OUT = reduce(container) {
    accumulator(it)

    return@reduce this
}

suspend fun <T> Publisher<T>.min(comparator: Comparator<T>): Result<T> =
    accumulate(BinaryOperator.minBy(comparator))

suspend fun <T : Comparable<T>> Publisher<T>.min(): Result<T> =
    min(naturalOrder())

suspend fun <T, R : Comparable<R>> Publisher<T>.minBy(extractor: (T) -> R?): Result<T> =
    min(compareBy(extractor))

suspend fun <T> Publisher<T>.max(comparator: Comparator<T>): Result<T> =
    accumulate(BinaryOperator.maxBy(comparator))

suspend fun <T : Comparable<T>> Publisher<T>.max(): Result<T> =
    max(naturalOrder())

suspend fun <T, R : Comparable<R>> Publisher<T>.maxBy(extractor: (T) -> R?): Result<T> =
    max(compareBy(extractor))

suspend fun <T> Publisher<T>.count(): Long =
    reduce(0) { this + 1 }

suspend fun <T> Publisher<T>.matching(predicate: (T) -> Boolean, type: Matching): Boolean =
    subscribe(
        complete = { yield(true) }
    ) {
        if (predicate(it) == type.stopOnPredicateMatches)
            yield(type.failResult)
    }

suspend fun <T> Publisher<T>.anyMatch(predicate: (T) -> Boolean): Boolean =
    matching(predicate, Matching.ANY)

suspend fun <T> Publisher<T>.allMatch(predicate: (T) -> Boolean): Boolean =
    matching(predicate, Matching.ALL)

suspend fun <T> Publisher<T>.noneMatch(predicate: (T) -> Boolean): Boolean =
    matching(predicate, Matching.NONE)

suspend fun <T> Publisher<T>.findFirst(): Result<T> =
    limit(1).subscribe(complete = { yield(Result.empty()) }) { yield(it.result()) }

suspend fun <T> Publisher<T>.first(): T = findFirst().get()

suspend fun <T, R : MutableCollection<T>> Publisher<T>.collect(
    container: R
): R = collect(container) { add(it) }

suspend fun <T> Publisher<T>.toMutableList(
    container: MutableList<T> = mutableListOf()
): MutableList<T> = collect(container)

suspend fun <T> Publisher<T>.toList(
    container: MutableList<T> = mutableListOf()
): List<T> = collect(container)

suspend fun <T> Publisher<T>.toMutableSet(
    container: MutableSet<T> = mutableSetOf()
): MutableSet<T> = collect(container)

suspend fun <T> Publisher<T>.toSet(
    container: MutableSet<T> = mutableSetOf()
): Set<T> = collect(container)

suspend inline fun <reified T> Publisher<T>.toTypedArray(): Array<T> = toList().toTypedArray()

suspend fun <T, K, V, MAP : MutableMap<K, V>> Publisher<T>.associate(
    container: MAP,
    mapper: MapBuilder<K, V, MAP>.(T) -> Unit
) = collect(MapBuilder(container)) { mapper(it) }.build()

suspend fun <T, K, V> Publisher<T>.associate(
    mapper: MapBuilder<K, V, MutableMap<K, V>>.(T) -> Unit
) = associate(mutableMapOf(), mapper)

suspend fun <K, V, MAP : MutableMap<K, V>> Publisher<K>.associateWith(
    container: MAP,
    mapper: (K) -> V
) = associate(container) { it to mapper(it) }

suspend fun <K, V> Publisher<K>.associateWith(
    mapper: (K) -> V
) = associateWith(mutableMapOf(), mapper)

suspend fun <K, V, MAP : MutableMap<K, V>> Publisher<V>.associateBy(
    container: MAP,
    mapper: (V) -> K
) = associate(container) { mapper(it) to it }

suspend fun <K, V> Publisher<V>.associateBy(
    mapper: (V) -> K
) = associateBy(mutableMapOf(), mapper)

@Suppress("UNCHECKED_CAST")
suspend fun <T, K, V, MAP : Multimap<K, V>> Publisher<T>.group(
    keys: MultimapBuilder.MultimapBuilderWithKeys<Any?> = MultimapBuilder.linkedHashKeys(),
    values: MultimapBuilder.MultimapBuilderWithKeys<Any?>.() -> MultimapBuilder<Any?, Any?>,
    mapper: MultiMapBuilder<K, V, MAP>.(T) -> Unit
) = collect(MultiMapBuilder(keys.values().build<K, V>() as MAP)) { mapper(it) }.build()

suspend fun <T, K, V> Publisher<T>.group(
    keys: MultimapBuilder.MultimapBuilderWithKeys<Any?> = MultimapBuilder.linkedHashKeys(),
    mapper: MultiMapBuilder<K, V, Multimap<K, V>>.(T) -> Unit
): Multimap<K, V> = group(keys, { arrayListValues() }, mapper)

suspend fun <K, V, MAP : Multimap<K, V>> Publisher<K>.groupWith(
    keys: MultimapBuilder.MultimapBuilderWithKeys<Any?> = MultimapBuilder.linkedHashKeys(),
    values: MultimapBuilder.MultimapBuilderWithKeys<Any?>.() -> MultimapBuilder<Any?, Any?>,
    mapper: (K) -> V
) = group<K, K, V, MAP>(keys, values) { it to mapper(it) }

suspend fun <K, V> Publisher<K>.groupWith(
    keys: MultimapBuilder.MultimapBuilderWithKeys<Any?> = MultimapBuilder.linkedHashKeys(),
    mapper: (K) -> V
): Multimap<K, V> = groupWith(keys, { arrayListValues() }, mapper)

suspend fun <K, V, MAP : Multimap<K, V>> Publisher<V>.groupBy(
    keys: MultimapBuilder.MultimapBuilderWithKeys<Any?> = MultimapBuilder.linkedHashKeys(),
    values: MultimapBuilder.MultimapBuilderWithKeys<Any?>.() -> MultimapBuilder<Any?, Any?>,
    mapper: (V) -> K
) = group<V, K, V, MAP>(keys, values) { mapper(it) to it }

suspend fun <K, V, MAP : Multimap<K, V>> Publisher<V>.groupBy(
    keys: MultimapBuilder.MultimapBuilderWithKeys<Any?> = MultimapBuilder.linkedHashKeys(),
    mapper: (V) -> K
): Multimap<K, V> = groupBy(keys, { arrayListValues() }, mapper)

@JvmName("sumByByte")
@OverloadResolutionByLambdaReturnType
suspend fun <T> Publisher<T>.sumBy(extractor: (T) -> Byte): Int =
    reduce(0) { this + extractor(it) }

@JvmName("sumByShort")
@OverloadResolutionByLambdaReturnType
suspend fun <T> Publisher<T>.sumBy(extractor: (T) -> Short): Int =
    reduce(0) { this + extractor(it) }

@JvmName("sumByInt")
@OverloadResolutionByLambdaReturnType
suspend fun <T> Publisher<T>.sumBy(extractor: (T) -> Int): Int =
    reduce(0) { this + extractor(it) }

@JvmName("sumByLong")
@OverloadResolutionByLambdaReturnType
suspend fun <T> Publisher<T>.sumBy(extractor: (T) -> Long): Long =
    reduce(0L) { this + extractor(it) }

@JvmName("sumByFloat")
@OverloadResolutionByLambdaReturnType
suspend fun <T> Publisher<T>.sumBy(extractor: (T) -> Float): Float =
    reduce(0.0f) { this + extractor(it) }

@JvmName("sumByDouble")
@OverloadResolutionByLambdaReturnType
suspend fun <T> Publisher<T>.sumBy(extractor: (T) -> Double): Double =
    reduce(0.0) { this + extractor(it) }

@JvmName("sumByBigInt")
@OverloadResolutionByLambdaReturnType
suspend fun <T> Publisher<T>.sumBy(extractor: (T) -> BigInteger): BigInteger =
    reduce(BigInteger.ZERO) { this + extractor(it) }

@JvmName("sumByBigDecimal")
@OverloadResolutionByLambdaReturnType
suspend fun <T> Publisher<T>.sumBy(extractor: (T) -> BigDecimal): BigDecimal =
    reduce(BigDecimal.ZERO) { this + extractor(it) }

private abstract class Average<IN, OUT : Number>(var value: OUT) : MutableCollection<IN> {
    var count: Long = 0

    override val size: Int get() = unsupported()
    override fun clear() = unsupported()
    override fun isEmpty(): Boolean = unsupported()
    override fun iterator(): MutableIterator<IN> = unsupported()
    override fun retainAll(elements: Collection<IN>): Boolean = unsupported()
    override fun removeAll(elements: Collection<IN>): Boolean = unsupported()
    override fun remove(element: IN): Boolean = unsupported()
    override fun containsAll(elements: Collection<IN>): Boolean = unsupported()
    override fun contains(element: IN): Boolean = unsupported()
    override fun addAll(elements: Collection<IN>): Boolean = unsupported()

    override fun add(element: IN): Boolean {
        inc(element)
        count++

        return true
    }

    abstract fun inc(it: IN)
    abstract fun calc(): OUT
}

@JvmName("averageOfByte")
suspend fun Publisher<Byte>.average(): Double =
    collect(object : Average<Byte, Double>(0.0) {
        override fun inc(it: Byte) {
            value += it
        }

        override fun calc(): Double = value / count
    }).calc()

@JvmName("averageOfShort")
suspend fun Publisher<Short>.average(): Double =
    collect(object : Average<Short, Double>(0.0) {
        override fun inc(it: Short) {
            value += it
        }

        override fun calc(): Double = value / count
    }).calc()

@JvmName("averageOfInt")
suspend fun Publisher<Int>.average(): Double =
    collect(object : Average<Int, Double>(0.0) {
        override fun inc(it: Int) {
            value += it
        }

        override fun calc(): Double = value / count
    }).calc()

@JvmName("averageOfLong")
suspend fun Publisher<Long>.average(): Double =
    collect(object : Average<Long, Double>(0.0) {
        override fun inc(it: Long) {
            value += it
        }

        override fun calc(): Double = value / count
    }).calc()

@JvmName("averageOfFloat")
suspend fun Publisher<Float>.average(): Double =
    collect(object : Average<Float, Double>(0.0) {
        override fun inc(it: Float) {
            value += it
        }

        override fun calc(): Double = value / count
    }).calc()

@JvmName("averageOfDouble")
suspend fun Publisher<Double>.average(): Double =
    collect(object : Average<Double, Double>(0.0) {
        override fun inc(it: Double) {
            value += it
        }

        override fun calc(): Double = value / count
    }).calc()

@JvmName("averageOfBigInteger")
suspend fun Publisher<BigInteger>.average(): BigDecimal =
    collect(object : Average<BigInteger, BigDecimal>(BigDecimal.ZERO) {
        override fun inc(it: BigInteger) {
            value += it.toBigDecimal()
        }

        override fun calc(): BigDecimal = value / count.toBigDecimal()
    }).calc()

@JvmName("averageOfBigDecimal")
suspend fun Publisher<BigDecimal>.average(): BigDecimal =
    collect(object : Average<BigDecimal, BigDecimal>(BigDecimal.ZERO) {
        override fun inc(it: BigDecimal) {
            value += it
        }

        override fun calc(): BigDecimal = value / count.toBigDecimal()
    }).calc()

@JvmName("sumOfByte")
@OverloadResolutionByLambdaReturnType
suspend fun Publisher<Byte>.sum(): Int =
    reduce(0) { this + it }

@JvmName("sumOfShort")
@OverloadResolutionByLambdaReturnType
suspend fun Publisher<Short>.sum(): Int =
    reduce(0) { this + it }

@JvmName("sumOfInt")
@OverloadResolutionByLambdaReturnType
suspend fun Publisher<Int>.sum(): Int =
    reduce(0) { this + it }

@JvmName("sumOfLong")
@OverloadResolutionByLambdaReturnType
suspend fun Publisher<Long>.sum(): Long =
    reduce(0L) { this + it }

@JvmName("sumOfFloat")
@OverloadResolutionByLambdaReturnType
suspend fun Publisher<Float>.sum(): Float =
    reduce(0.0f) { this + it }

@JvmName("sumOfDouble")
@OverloadResolutionByLambdaReturnType
suspend fun Publisher<Double>.sum(): Double =
    reduce(0.0) { this + it }

@JvmName("sumOfBigInt")
@OverloadResolutionByLambdaReturnType
suspend fun Publisher<BigInteger>.sum(): BigInteger =
    reduce(BigInteger.ZERO) { this + it }

@JvmName("sumOfBigDecimal")
@OverloadResolutionByLambdaReturnType
suspend fun Publisher<BigDecimal>.sum(): BigDecimal =
    reduce(BigDecimal.ZERO) { this + it }

fun <T> Publisher<T>.iterator(batch: Long): RxIterator<T> =
    BatchedIterator<T>(batch).apply { this@iterator.subscribe(this) }

operator fun <T> Publisher<T>.iterator(): RxIterator<T> = iterator(70)

suspend fun <T> Publisher<T>.discard(): Unit =
    subscribe(complete = { yield(Unit) }) { }

suspend fun <T> Publisher<T>.forEach(
    block: (T) -> Unit
): Unit = this@forEach.subscribe(complete = { yield(Unit) }) { block(it) }

suspend inline infix fun <T> Publisher<T>.iterate(
    block: RxIterator<T>.(T) -> Unit
) = iterator().iterate(block)