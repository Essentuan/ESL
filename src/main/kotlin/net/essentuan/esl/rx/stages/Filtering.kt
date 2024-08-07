package net.essentuan.esl.rx.stages

import net.essentuan.esl.rx.Stage
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

class Filtering<T>(
    val predicate: suspend (T) -> Boolean,
    upstream: Publisher<T>,
    downstream: Subscriber<in T>
) : Stage<T, T>(upstream, downstream) {
    override suspend fun generate() {
        for (e in this)
            if (predicate(e))
                yield(e)
    }

    class Not<T>(
        val predicate: suspend (T) -> Boolean,
        upstream: Publisher<T>,
        downstream: Subscriber<in T>
    ) : Stage<T, T>(upstream, downstream) {
        override suspend fun generate() {
            for (e in this)
                if (!predicate(e))
                    yield(e)
        }
    }
}