package net.essentuan.esl.collector.maps.multi

import net.essentuan.esl.collector.SimpleCollector
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import java.util.function.Function

class MultimapCollector<T, K, V> @JvmOverloads constructor(
    private val keyMapper: Function<T, K>,
    private val valueMapper: Function<T, V> = Function { value: T -> value as V }
) : SimpleCollector<T, Multimap<K, V>, Multimap<K, V>>() {
    override fun supply(): Multimap<K, V> {
        return MultimapBuilder.linkedHashKeys()
            .arrayListValues().build()
    }

    override fun accumulate(container: Multimap<K, V>, value: T) {
        container.put(
            keyMapper.apply(value),
            valueMapper.apply(value)
        )
    }

    override fun combine(left: Multimap<K, V>, right: Multimap<K, V>): Multimap<K, V> {
        left.putAll(right)

        return left
    }

    override fun finish(container: Multimap<K, V>): Multimap<K, V> {
        return container
    }
}
