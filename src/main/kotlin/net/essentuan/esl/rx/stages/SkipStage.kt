package net.essentuan.esl.rx.stages

import net.essentuan.esl.rx.Stage
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

class SkipStage<T>(
    var capacity: Long,
    upstream: Publisher<T>,
    downstream: Subscriber<in T>
) : Stage<T, T>(upstream, downstream) {
    override suspend fun generate() {
        for (e in this)
            if (--capacity <= 0)
                yield(e)
    }

    @Synchronized
    override fun request(n: Long) {
        require(n >= 0) { "n must be positive!" }

        super.request(capacity + n)
    }
}