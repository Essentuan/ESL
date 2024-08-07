@file:OptIn(ExperimentalTypeInference::class)

package net.essentuan.esl.rx

import kotlinx.coroutines.CoroutineScope
import net.essentuan.esl.rx.stages.BatchedStage
import net.essentuan.esl.rx.stages.DistinctStage
import net.essentuan.esl.rx.stages.Filtering
import net.essentuan.esl.rx.stages.FlatMapStage
import net.essentuan.esl.rx.stages.LimitStage
import net.essentuan.esl.rx.stages.MappingStage
import net.essentuan.esl.rx.stages.SkipStage
import net.essentuan.esl.rx.stages.SortingStage
import net.essentuan.esl.rx.stages.WhileStage
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import kotlin.experimental.ExperimentalTypeInference

inline fun <IN, OUT> gather(
    crossinline stage: (Subscriber<in OUT>) -> Stage<IN, OUT>
): Publisher<OUT> = Publisher { stage(it).subscribe() }

fun <T> Publisher<T>.batched(size: Long): Publisher<T> =
    gather { BatchedStage(size, this, it) }

fun <T> Publisher<T>.filter(predicate: suspend (T) -> Boolean): Publisher<T> =
    gather { Filtering(predicate, this, it) }

fun <T> Publisher<T>.filterNot(predicate: suspend (T) -> Boolean): Publisher<T> =
    gather { Filtering.Not(predicate, this, it) }

@Suppress("UNCHECKED_CAST")
fun <T: Any> Publisher<T?>.filterNotNull(): Publisher<T> = filterNot { it == null } as Publisher<T>

fun <T, U> Publisher<T>.map(transformer: suspend (T) -> U): Publisher<U> =
    gather { MappingStage(transformer, this, it) }

fun <T, U> Publisher<T>.merge(transformer: suspend CoroutineScope.(T) -> U): Publisher<U> =
    gather { MappingStage.Merging(transformer, this, it) }

fun <T, U> Publisher<T>.flatMap(transformer: suspend (T) -> Publisher<U>): Publisher<U> =
    gather { FlatMapStage(transformer, this, it) }

fun <T, U> Publisher<T>.merging(transformer: suspend CoroutineScope.(T) -> Publisher<U>): Publisher<U> =
    gather { FlatMapStage.Merging(transformer, this, it) }

fun <T> Publisher<T>.prepend(vararg publishers: Publisher<T>): Publisher<T> =
    Publishers.concat(*publishers, this)

fun <T> Publisher<T>.append(vararg publishers: Publisher<T>): Publisher<T> =
    Publishers.concat(this, *publishers)

fun <T> Publisher<T>.merge(vararg publishers: Publisher<T>): Publisher<T> =
    Publishers.merge(this, *publishers)

@Suppress("UNCHECKED_CAST")
fun <T, U> Publisher<T>.cast(): Publisher<U> = this as Publisher<U>

fun <T> Publisher<T>.distinct(extractor: suspend (T) -> Any? = { it }): Publisher<T> =
    gather { DistinctStage(extractor, this, it) }

fun <T> Publisher<T>.sortedWith(comparator: Comparator<T>): Publisher<T> =
    gather { SortingStage(comparator, this, it) }

fun <T> Publisher<T>.sortedDescending(comparator: Comparator<T>): Publisher<T> =
    gather { SortingStage(comparator.reversed(), this, it) }

fun <T, R: Comparable<R>> Publisher<T>.sortedBy(extractor: (T) -> R?): Publisher<T> =
    sortedWith(compareBy(extractor))

fun <T, R: Comparable<R>> Publisher<T>.sortedByDescending(extractor: (T) -> R?): Publisher<T> =
    sortedWith(compareByDescending(extractor))

fun <T: Comparable<T>> Publisher<T>.sorted() = sortedWith(naturalOrder())

fun <T: Comparable<T>> Publisher<T>.sortedDescending() = sortedWith(reverseOrder())

fun <T> Publisher<T>.peek(block: (T) -> Unit): Publisher<T> = Publisher {
    this.subscribe(object : Subscriber<T> {
        override fun onNext(value: T) {
            block(value)
            it.onNext(value)
        }

        override fun onSubscribe(s: Subscription) = it.onSubscribe(s)
        override fun onError(t: Throwable?) = it.onError(t)
        override fun onComplete() = it.onComplete()
    })
}

fun <T> Publisher<T>.limit(maxSize: Long): Publisher<T> = gather { LimitStage(maxSize, this, it) }

fun <T> Publisher<T>.skip(n: Long): Publisher<T> = gather { SkipStage(n, this, it) }

fun <T> Publisher<T>.takeWhile(predicate: suspend (T) -> Boolean): Publisher<T> =
    gather { WhileStage.Taking(predicate, this, it) }

fun <T> Publisher<T>.dropWhile(predicate: suspend (T) -> Boolean): Publisher<T> =
    gather { WhileStage.Dropping(predicate, this, it) }