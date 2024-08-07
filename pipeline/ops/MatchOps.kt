package net.essentuan.esl.async.pipeline.ops

import net.essentuan.esl.async.pipeline.Gatherer
import java.util.function.Predicate

class MatchOps<T>(private val predicate: Predicate<in T>, private val op: Kind) : Gatherer<T, Boolean>() {

    override fun onNext(value: T) {
        if (predicate.test(value) == op.stopOnPredicateMatches) {
            accept(op.failResult)
            close()
        }
    }

    override fun onComplete() {
        accept(op.failResult)

        complete()
    }

    enum class Kind(
        val stopOnPredicateMatches: Boolean,
        val failResult: Boolean
    ) {
        ANY(true, true),
        ALL(false, false),
        NONE(true, false)
    }
}
