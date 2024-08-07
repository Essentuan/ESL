package net.essentuan.esl.rx.stages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import net.essentuan.esl.coroutines.await
import net.essentuan.esl.rx.Stage
import net.essentuan.esl.rx.iterator
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

class FlatMapStage<IN, OUT>(
    val transformer: suspend (IN) -> Publisher<OUT>,
    upstream: Publisher<IN>,
    downstream: Subscriber<in OUT>
) : Stage<IN, OUT>(upstream, downstream) {
    override suspend fun generate() {
        for (e in this)
            yieldAll(transformer(e))
    }

    class Merging<IN, OUT>(
        val transformer: suspend CoroutineScope.(IN) -> Publisher<OUT>,
        upstream: Publisher<IN>,
        downstream: Subscriber<in OUT>
    ) : Stage<IN, OUT>(upstream, downstream) {
        override suspend fun generate() = coroutineScope {
            await {
                for (e in this@Merging)
                    +launch {
                        for (value in transformer(e))
                            yield(value)
                    }
            }
        }
    }
}