package net.essentuan.esl.rx.stages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.essentuan.esl.rx.Stage
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

class MappingStage<IN, OUT>(
    val transformer: suspend (IN) -> OUT,
    upstream: Publisher<IN>,
    downstream: Subscriber<in OUT>
) : Stage<IN, OUT>(upstream, downstream) {
    override suspend fun generate() {
        for (e in this)
            yield(transformer(e))
    }

    class Merging<IN, OUT>(
        val transformer: suspend CoroutineScope.(IN) -> OUT,
        upstream: Publisher<IN>,
        downstream: Subscriber<in OUT>
    ) : Stage<IN, OUT>(upstream, downstream) {
        override suspend fun generate() = coroutineScope {
            for (e in this@Merging)
                launch {
                    yield(transformer(e))
                }
        }
    }
}