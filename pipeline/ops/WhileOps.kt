package net.essentuan.esl.async.pipeline.ops

import net.essentuan.esl.async.pipeline.Gatherer
import java.util.function.Predicate

open class WhileOps<T>(val predicate: Predicate<in T>) : Gatherer<T, T>() {
    var active: Boolean = true

    class Taking<T>(predicate: Predicate<in T>) : WhileOps<T>(predicate) {
        override fun onNext(value: T) {
            if (!active) return

            if (predicate.test(value)) accept(value)
            else close()
        }
    }

    class Dropping<T>(predicate: Predicate<in T>) : WhileOps<T>(predicate) {
        override fun onNext(value: T) {
            if (!active) accept(value)
            else if (!predicate.test(value)) {
                accept(value)
                active = false
            }
        }
    }
}
