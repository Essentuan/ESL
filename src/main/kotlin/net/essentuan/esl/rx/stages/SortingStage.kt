package net.essentuan.esl.rx.stages

import net.essentuan.esl.rx.Stage
import net.essentuan.esl.rx.toMutableList
import net.essentuan.esl.rx.yieldAll
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber

class SortingStage<T>(
    val comparator: Comparator<T>,
    upstream: Publisher<T>,
    downstream: Subscriber<in T>
) : Stage<T, T>(upstream, downstream) {
    override suspend fun generate() {
        val list = upstream.toMutableList()
        list.sortWith(comparator)

        yieldAll(list)
    }
}