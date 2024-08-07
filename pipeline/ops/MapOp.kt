package net.essentuan.esl.async.pipeline.ops

import net.essentuan.esl.future.api.Future
import net.essentuan.esl.async.pipeline.Gatherer
import java.util.function.Function

class MapOp<In, Out>(private val mapper: Function<in In, out Out>) : Gatherer<In, Out>() {
    override fun onNext(value: In) {
        accept(if (value == null) value as Out else mapper.apply(value))
    }

    class Async<In, Out>(private val mapper: Function<In, out Future<Out>>) : Gatherer<In, Out>() {
        private val outbound: MutableSet<Any?> = HashSet()

        @Suppress("UNCHECKED_CAST", "ALWAYS_NULL", "KotlinConstantConditions")
        override fun onNext(value: In) {
            if (value == null) accept(value as Out)

            val future: Future<Out> = mapper.apply(value)
            synchronized(outbound) {
                outbound.add(future)
            }

            future.exec { synchronized(outbound) { outbound.remove(future) } }
                .capture(this::error)
                .then(this::accept)
                .run { onComplete() }
        }

        override fun onComplete() = synchronized(outbound) {
            if (outbound.isEmpty())
                complete()
        }
    }
}
