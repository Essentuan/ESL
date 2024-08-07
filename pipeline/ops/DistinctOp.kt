package net.essentuan.esl.async.pipeline.ops

import net.essentuan.esl.async.pipeline.Gatherer

class DistinctOp<T>(val extractor: (T) -> Any?) : Gatherer<T, T>() {
    private var seen: MutableSet<Any?> = HashSet()

    override fun onNext(value: T) {
        synchronized(seen) {
            if (seen.add(extractor(value)))
                accept(value)
        }
    }
}
