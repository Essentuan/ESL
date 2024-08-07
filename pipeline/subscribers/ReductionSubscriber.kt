package net.essentuan.esl.async.pipeline.subscribers

import net.essentuan.esl.async.pipeline.Subscriber
import java.util.function.BiFunction

class ReductionSubscriber<In, Out>(identity: Out?, val accumulator: BiFunction<Out, in In, Out>) :
    Subscriber<In, Out>() {
    var out: Out?

    init {
        out = identity
    }

    override fun onNext(value: In) {
        out = if (out == null) value as Out
        else accumulator.apply(out!!, value)
    }

    override fun result(): Out {
        return out!!
    }
}
