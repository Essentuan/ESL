package net.essentuan.esl.rx.stages

import net.essentuan.esl.rx.Stage
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

class BatchedStage<T>(
    var size: Long,
    upstream: Publisher<T>,
    downstream: Subscriber<in T>
) : Stage<T, T>(upstream, downstream) {
    override suspend fun generate() {
        batchSize = size

        for (e in this)
            yield(e)
    }
}