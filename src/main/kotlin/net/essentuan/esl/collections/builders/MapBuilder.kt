package net.essentuan.esl.collections.builders

import org.checkerframework.checker.units.qual.K

@JvmInline
value class MapBuilder<K, V, MAP: MutableMap<K, V>>(private val map: MAP) {
    infix fun K.to(obj: V) = map.put(this, obj)

    fun build(): MAP = map
}

inline fun <K, V> map(
    map: MutableMap<K, V> = LinkedHashMap(),
    init: MapBuilder<K, V, MutableMap<K, V>>.() -> Unit
): Map<K, V> {
    return MapBuilder(map).apply(init).build()
}

inline fun <K, V> mutableMap(
    map: MutableMap<K, V> = LinkedHashMap(),
    init: MapBuilder<K, V, MutableMap<K, V>>.() -> Unit
): MutableMap<K, V> {
    return MapBuilder(map).apply(init).build()
}