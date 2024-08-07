package net.essentuan.esl.async.pipeline.gatherers

import net.essentuan.esl.async.pipeline.Gatherer
import net.essentuan.esl.async.pipeline.Pipeline

class ExpectedSize<T>(val range: IntRange) : Gatherer<T, T>() {
    private val results: MutableList<T> = ArrayList()

    override fun onNext(value: T) {
        synchronized(results) {
            results.add(value)
        }
    }

    override fun onComplete() {
        if (range.contains(results.size)) synchronized(results) {
            for (value in results) accept(value)
        }

        complete()
    }
}

fun <T> Pipeline<T>.expect(size: Int): Pipeline<T> = expect(size..size)

fun <T> Pipeline<T>.expect(range: IntRange): Pipeline<T> = gather(ExpectedSize(range))