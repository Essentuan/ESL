package net.essentuan.esl.async.pipeline.ops

import net.essentuan.esl.async.pipeline.Gatherer
import kotlin.math.min

class LimitOp<T>(private val limit: Long) : Gatherer<T, T>() {
    private var accepted: Long = 0

    override fun onNext(value: T) {
        if (accepted >= limit) return

        accept(value)
        accepted++

        if (accepted >= limit) close()
    }

    override fun onRequest(value: Long): Long {
        return min(value.toDouble(), limit.toDouble()).toLong()
    }
}
