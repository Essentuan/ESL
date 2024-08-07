package net.essentuan.esl.rx.stages

import net.essentuan.esl.collections.synchronized
import net.essentuan.esl.rx.Stage
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

class DistinctStage<T>(
    val extractor: suspend (T) -> Any?,
    upstream: Publisher<T>,
    downstream: Subscriber<in T>
) : Stage<T, T>(upstream, downstream) {
    val seen = mutableSetOf<Any?>().synchronized()

    override suspend fun generate() {
        for (e in this) {
            if (seen.add(extractor(e)))
                yield(e)
        }
    }
}