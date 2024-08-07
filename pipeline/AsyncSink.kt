package net.essentuan.esl.async.pipeline

import reactor.core.Disposable
import reactor.core.publisher.FluxSink
import reactor.util.context.Context
import java.util.function.LongConsumer

internal class AsyncSink<T>(private val sink: FluxSink<T>) : Sink<T> {
    override var isOpen: Boolean = true
        private set

    private var onDispose = Disposable {}
    private var onCancel = Disposable {}

    init {
        sink.onDispose {
            isOpen = false
            onDispose.dispose()
        }.onCancel {
            isOpen = false
            onCancel.dispose()
        }
    }

    override fun next(t: T): Sink<T> {
        if (isOpen) sink.next(t)

        return this
    }

    override fun complete() {
        if (!isOpen) return

        isOpen = false
        sink.complete()
    }

    override fun error(e: Throwable) {
        if (isOpen) sink.error(e)
    }

    @Deprecated("")
    override fun currentContext(): Context = sink.currentContext()

    override fun requestedFromDownstream(): Long {
        return sink.requestedFromDownstream()
    }

    override fun isCancelled(): Boolean = sink.isCancelled

    override fun onRequest(consumer: LongConsumer): Sink<T> {
        sink.onRequest(consumer)

        return this
    }

    override fun onCancel(d: Disposable): Sink<T> {
        onCancel = d

        return this
    }

    override fun onDispose(d: Disposable): Sink<T> {
        onDispose = d

        return this
    }
}
