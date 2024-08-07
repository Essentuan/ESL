package net.essentuan.esl.async.pipeline

import reactor.core.Disposable
import reactor.core.publisher.FluxSink
import java.util.function.LongConsumer
import java.util.stream.Stream

interface Sink<T> : FluxSink<T> {
    fun yield(value: T) {
        next(value)
    }

    fun yieldAll(iterator: Iterator<T>) = iterator.forEach(this::yield)

    fun yieldAll(iterable: Iterable<T>) = iterable.forEach(this::yield)

    fun yieldAll(sequence: Sequence<T>) = sequence.forEach(this::yield)

    fun yieldAll(stream: Stream<T>) = stream.forEach(this::yield)

    suspend fun yieldAll(pipeline: Pipeline<T>) = pipeline.forEach(this::yield)

    override fun next(value: T): Sink<T>

    override fun onRequest(consumer: LongConsumer): Sink<T>

    override fun onCancel(d: Disposable): Sink<T>

    override fun onDispose(d: Disposable): Sink<T>

    val isOpen: Boolean
}
