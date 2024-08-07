package net.essentuan.esl.rx

import net.essentuan.esl.other.unsupported
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

abstract class Stage<IN, OUT>(
    val upstream: Publisher<IN>,
    downstream: Subscriber<in OUT>
) : Generator<OUT>(downstream) {
    @Suppress("UNCHECKED_CAST")
    private var iter: RxIterator<IN> = Missing as RxIterator<IN>

    var batchSize: Long = 10
        set(value) {
            if (iter != Missing)
                iter.batchSize = value

            field = value
        }

    override fun request(n: Long) {
        require(n >= 0) { "n must be positive!" }

        batchSize = maxOf(n, batchSize)

        super.request(n)
    }

    operator fun iterator(): RxIterator<IN> {
        if (iter == Missing)
            iter = upstream.iterator(batchSize)

        return iter
    }
}

private object Missing : RxIterator<Any?> {
    override var batchSize: Long
        get() = unsupported()
        set(value) = unsupported()
    override suspend fun hasNext(): Boolean = unsupported()
    override fun next(): Any = unsupported()
}