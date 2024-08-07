package net.essentuan.esl.collector

import net.essentuan.esl.annotated.AnnotatedType
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector

abstract class SimpleCollector<T, A, R> : AnnotatedType(optional(Characteristics())) , Collector<T, A, R> {
    protected abstract fun supply(): A

    protected abstract fun accumulate(container: A, value: T)

    protected abstract fun combine(left: A, right: A): A

    protected abstract fun finish(container: A): R

    override fun supplier(): Supplier<A> {
        return Supplier { this.supply() }
    }

    override fun accumulator(): BiConsumer<A, T> {
        return BiConsumer<A, T> { container: A, value: T -> this.accumulate(container, value) }
    }

    override fun combiner(): BinaryOperator<A> {
        return BinaryOperator<A> { left: A, right: A -> this.combine(left, right) }
    }

    override fun finisher(): Function<A, R> {
        return Function { container: A -> this.finish(container) }
    }

    override fun characteristics(): Set<Collector.Characteristics> {
        return setOf(*this.get(Characteristics::class).value)
    }

    companion object {
        fun <T, A, R> of(
            supplier: Supplier<A>,
            accumulator: BiConsumer<A, T>,
            combiner: BinaryOperator<A>,
            finisher: Function<A, R>,
            vararg characteristics: Collector.Characteristics
        ): Collector<T, A, R> {
            return object : Collector<T, A, R> {
                override fun supplier(): Supplier<A> {
                    return supplier
                }

                override fun accumulator(): BiConsumer<A, T> {
                    return accumulator
                }

                override fun combiner(): BinaryOperator<A> {
                    return combiner
                }

                override fun finisher(): Function<A, R> {
                    return finisher
                }

                override fun characteristics(): Set<Collector.Characteristics> {
                    return setOf(*characteristics)
                }
            }
        }
    }
}
