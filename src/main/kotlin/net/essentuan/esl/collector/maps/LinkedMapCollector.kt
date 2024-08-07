package net.essentuan.esl.collector.maps

import net.essentuan.esl.collector.SimpleCollector

class LinkedMapCollector<T, K, V>(
    private val keyMapper: (T) -> K,
    private val valueMapper: (T) -> V
) : SimpleCollector<T, MutableMap<K, V>, MutableMap<K, V>>() {

    override fun supply(): MutableMap<K, V> {
        return LinkedHashMap()
    }

    override fun accumulate(container: MutableMap<K, V>, value: T) {
        container[keyMapper(value)] = valueMapper(value)
    }

    override fun combine(left: MutableMap<K, V>, right: MutableMap<K, V>): MutableMap<K, V> {
        left.putAll(right)

        return left
    }

    override fun finish(container: MutableMap<K, V>): MutableMap<K, V> {
        return container
    }
}
