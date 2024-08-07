package net.essentuan.esl.rx.producers

import net.essentuan.esl.rx.Producer
import org.reactivestreams.Subscriber

class IteratorProducer<T>(
    downstream: Subscriber<in T>,
    val iterator: Iterator<T>
) : Producer<T>(downstream) {
    override fun produce() {
        while (requested > 0 && iterator.hasNext())
            iterator.next().yield()

        if (!iterator.hasNext()) {
            complete()
        }
    }
}
