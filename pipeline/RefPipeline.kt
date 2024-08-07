package net.essentuan.esl.async.pipeline

import kotlinx.coroutines.suspendCancellableCoroutine
import net.essentuan.esl.future.api.Future
import net.essentuan.esl.async.pipeline.extensions.pipe
import net.essentuan.esl.async.pipeline.ops.CloseOp
import net.essentuan.esl.async.pipeline.ops.DistinctOp
import net.essentuan.esl.async.pipeline.ops.FilterOp
import net.essentuan.esl.async.pipeline.ops.LimitOp
import net.essentuan.esl.async.pipeline.ops.MapOp
import net.essentuan.esl.async.pipeline.ops.MatchOps
import net.essentuan.esl.async.pipeline.ops.PeekOp
import net.essentuan.esl.async.pipeline.ops.SkipOp
import net.essentuan.esl.async.pipeline.ops.WhileOps
import net.essentuan.esl.async.pipeline.subscribers.CountSubscriber
import net.essentuan.esl.async.pipeline.subscribers.ForEachSubscriber
import net.essentuan.esl.async.pipeline.subscribers.ReductionSubscriber
import net.essentuan.esl.iteration.extensions.iterable
import net.essentuan.esl.other.Result
import net.essentuan.esl.other.result
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import reactor.core.publisher.Flux
import java.util.Spliterator
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.BinaryOperator
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Stream
import kotlin.coroutines.resume

class RefPipeline<T>(private val faucet: Gatherer<*, T>) : Pipeline<T> {
    override fun filter(predicate: Predicate<in T>): Pipeline<T> = gather(FilterOp(predicate))

    override fun <R> map(mapper: Function<in T, out R>): Pipeline<R> = gather(
        MapOp(
            mapper
        )
    )

    override fun <R> await(mapper: Function<T, out Future<R>>): Pipeline<R> = gather(MapOp.Async(mapper))

    override fun <R> cast(cls: Class<R>): Pipeline<R> = map(cls::cast)

    override fun <R> cast(): Pipeline<R> = map { t -> t as R }

    override fun <R> flatMap(mapper: (T) -> Sequence<R>): Pipeline<R> = flux().flatMapIterable { mapper(it).asIterable() }.pipe()

    override fun concat(vararg publishers: Publisher<T>): Pipeline<T> = Pipeline.join(this, *publishers)

    override fun concat(vararg streams: Stream<T>): Pipeline<T> = concat(Flux.fromArray(streams).flatMapIterable { it.iterable() })

    override fun concat(vararg streams: Sequence<T>): Pipeline<T> = concat(Flux.fromArray(streams).flatMapIterable { it.asIterable() })

    override fun distinct(extractor: (T) -> Any?): Pipeline<T> = gather(DistinctOp(extractor))

    override fun sorted(comparator: Comparator<in T>): Pipeline<T> = flux().sort(comparator).pipe()

    override fun sorted(): Pipeline<T> = flux().sort().pipe()

    override fun limit(maxSize: Long): Pipeline<T> = gather(LimitOp(maxSize))

    override fun skip(n: Long): Pipeline<T> = gather(SkipOp(n))

    override fun peek(action: Consumer<in T>): Pipeline<T> = gather(PeekOp(action))

    override fun dropWhile(predicate: Predicate<in T>): Pipeline<T> = gather(WhileOps.Dropping(predicate))

    override fun takeWhile(predicate: Predicate<in T>): Pipeline<T> = gather(WhileOps.Taking(predicate))

    override fun onClose(closeHandler: Runnable): Pipeline<T> = gather(CloseOp(closeHandler))

    override suspend fun <U> reduce(
        identity: U,
        accumulator: BiFunction<U, in T, U>,
        combiner: BinaryOperator<U>
    ): U = attach(ReductionSubscriber(identity, accumulator)).await()

    override suspend fun reduce(identity: T, accumulator: BinaryOperator<T>): T =
        attach(ReductionSubscriber(identity, accumulator)).await()

    override suspend fun reduce(accumulator: BinaryOperator<T>): Result<T> =
        attach(ReductionSubscriber(null, accumulator)).await().result()

    override suspend fun <R, A> collect(collector: Collector<in T, A, R>): R {
        val accumulator = collector.accumulator()

        return this.reduce(collector.supplier().get(), { left: A, right: T ->
            accumulator.accept(left, right)
            left
        }, collector.combiner()).run {
            collector.finisher().apply(this)
        }
    }

    override suspend fun <R> collect(
        supplier: Supplier<R>,
        accumulator: BiConsumer<R, in T>,
        combiner: BiConsumer<R, R>
    ): R {
        return reduce(supplier.get(), { left: R, right: T ->
            accumulator.accept(left, right)
            left
        }, { left: R, right: R ->
            combiner.accept(left, right)
            left
        })
    }

    override suspend fun count(): Long = attach(CountSubscriber()).await()

    override suspend fun findFirst(): Result<T> = suspendCancellableCoroutine {
        subscribe(object : RxSubscriber<T> {
            override fun onSubscribe(s: Subscription) = s.request(1)

            override fun onError(t: Throwable) {
                it.cancel(t)
            }

            override fun onComplete() {
                it.resume(Result.empty())
            }

            override fun onNext(value: T) {
                it.resume(value.result())
            }
        })
    }

    private suspend fun match(predicate: Predicate<in T>, kind: MatchOps.Kind): Boolean =
        gather(MatchOps(predicate, kind)).first()


    override suspend fun noneMatch(predicate: Predicate<in T>): Boolean = match(predicate, MatchOps.Kind.NONE)

    override suspend fun allMatch(predicate: Predicate<in T>): Boolean = match(predicate, MatchOps.Kind.ALL)

    override suspend fun anyMatch(predicate: Predicate<in T>): Boolean = match(predicate, MatchOps.Kind.ANY)

    override suspend fun max(comparator: Comparator<in T>): Result<T> =
        reduce(BinaryOperator.maxBy(comparator))

    override suspend fun min(comparator: Comparator<in T>): Result<T> =
        reduce(BinaryOperator.maxBy(comparator))

    override fun flux(): Flux<T> = Flux.from(faucet)

    override fun stream(): Stream<T> = flux().toStream()

    override fun iterator(): Iterator<T> = flux().toIterable().iterator()

    override fun spliterator(): Spliterator<T> = flux().toIterable().spliterator()

    override suspend fun forEach(action: Consumer<in T>) = attach(ForEachSubscriber(action)).await()

    override fun <S : Subscriber<in T>> attach(subscriber: S): S = subscriber.also { faucet.subscribe(subscriber) }

    override fun <U> gather(gatherer: Gatherer<T, U>): Pipeline<U> = gatherer.bind(faucet).pipe()

    override fun close() = faucet.close()
}