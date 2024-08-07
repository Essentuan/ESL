package net.essentuan.esl.collector.maps

import net.essentuan.esl.collector.SimpleCollector
import net.essentuan.esl.unsafe
import java.util.EnumMap
import java.util.function.Function

class EnumMapCollector<T, K : Enum<K>, V>(
    private val cls: Class<K>,
    private val keyMapper: Function<T, K?>,
    private val valueMapper: Function<T, V?>
) : SimpleCollector<T, MutableMap<K, V>, MutableMap<K, V>>() {
    override fun supply(): MutableMap<K, V> {
        return EnumMap(cls)
    }

    override fun accumulate(container: MutableMap<K, V>, value: T) {
        unsafe {
            val key: K? = keyMapper.apply(value)
            val v: V? = valueMapper.apply(value)

            if (key != null && v != null) container[key] = v
        }
    }

    override fun combine(left: MutableMap<K, V>, right: MutableMap<K, V>): MutableMap<K, V> {
        left.putAll(right)

        return left
    }

    override fun finish(container: MutableMap<K, V>): MutableMap<K, V> {
        return container
    }
}