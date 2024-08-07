package net.essentuan.esl.async.pipeline.ops

import net.essentuan.esl.async.pipeline.Gatherer
import kotlin.math.min

class SkipOp<T>(private val skip: Long) : Gatherer<T, T>() {
    private var processed: Long = 0

    override fun onNext(value: T) {
        if (processed < skip) processed++
        else accept(value)
    }

    override fun onRequest(value: Long): Long {
        return min((value + skip).toInt(), Int.MAX_VALUE).toLong()
    }
}
