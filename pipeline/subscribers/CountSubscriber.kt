package net.essentuan.esl.async.pipeline.subscribers

import net.essentuan.esl.async.pipeline.Subscriber

class CountSubscriber<T> : Subscriber<T, Long>() {
    private var count: Long = 0

    override fun result(): Long {
        return count
    }

    override fun onNext(t: T) {
        count++
    }
}
