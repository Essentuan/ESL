package net.essentuan.esl.rx.stages

import net.essentuan.esl.rx.Stage
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

abstract class WhileStage<T>(
    val predicate: suspend (T) -> Boolean,
    upstream: Publisher<T>,
    downstream: Subscriber<in T>
) : Stage<T, T>(upstream, downstream) {
    class Taking<T>(
        predicate: suspend (T) -> Boolean,
        upstream: Publisher<T>,
        downstream: Subscriber<in T>
    ) : WhileStage<T>(predicate, upstream, downstream) {
        override suspend fun generate() {
            for (e in this) {
                if (!predicate(e))
                    break

                yield(e)
            }
        }
    }

    class Dropping<T>(
        predicate: suspend (T) -> Boolean,
        upstream: Publisher<T>,
        downstream: Subscriber<in T>
    ) : WhileStage<T>(predicate, upstream, downstream) {
        override suspend fun generate() {
            val iterator = iterator()

            while (iterator.hasNext()) {
                val next = iterator.next()

                if (!predicate(next)) {
                    yield(next)
                    break
                }
            }

            while (iterator.hasNext())
                yield(iterator.next())
        }
    }
}