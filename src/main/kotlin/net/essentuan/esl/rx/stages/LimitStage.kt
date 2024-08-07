package net.essentuan.esl.rx.stages

import net.essentuan.esl.rx.Stage
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import kotlin.math.min

class LimitStage<T>(
    var capacity: Long,
    upstream: Publisher<T>,
    downstream: Subscriber<in T>
) : Stage<T, T>(upstream, downstream) {
    override suspend fun generate() {
        val iterator = iterator()

        while (capacity > 0 && iterator.hasNext()) {
            yield(iterator.next())
            capacity--
        }
    }

    override fun request(n: Long) {
        require(n >= 0) { "n must be positive!" }

        super.request(min(capacity, n))
    }
}