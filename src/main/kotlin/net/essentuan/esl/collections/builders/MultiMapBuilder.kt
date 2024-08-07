package net.essentuan.esl.collections.builders

import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.MultimapBuilder.ListMultimapBuilder
import com.google.common.collect.SetMultimap
import com.google.common.collect.SortedSetMultimap
import net.essentuan.esl.iteration.extensions.iterable
import java.util.stream.Stream

@JvmInline
value class MultiMapBuilder<K, V, MAP: Multimap<K, V>>(private val map: MAP) {
    infix fun K.to(obj: V) = map.put(this, obj)

    infix fun K.to(obj: Iterable<V>) = map.putAll(this, obj)

    infix fun K.to(obj: Stream<V>) = map.putAll(this, obj.iterable())

    infix fun K.to(obj: Sequence<V>) = map.putAll(this, obj.asIterable())

    fun build(): MAP = map
}

inline fun <K, V> multimap(
    keys: MultimapBuilder.MultimapBuilderWithKeys<Any?> = MultimapBuilder.linkedHashKeys(),
    init: MultiMapBuilder<K, V, *>.() -> Unit
): Multimap<K, V> = MultiMapBuilder(keys.arrayListValues().build<K, V>()).apply(init).build()

inline fun <K, V> listMultimap(
    keys: MultimapBuilder.MultimapBuilderWithKeys<Any?> = MultimapBuilder.linkedHashKeys(),
    values: (MultimapBuilder.MultimapBuilderWithKeys<Any?>) -> ListMultimapBuilder<Any?, Any?> = { it.arrayListValues() } ,
    init: MultiMapBuilder<K, V, ListMultimap<K, V>>.() -> Unit
): ListMultimap<K, V> = MultiMapBuilder(values(keys).build<K, V>()).apply(init).build()

inline fun <K, V> setMultimap(
    keys: MultimapBuilder.MultimapBuilderWithKeys<Any?> = MultimapBuilder.linkedHashKeys(),
    values: (MultimapBuilder.MultimapBuilderWithKeys<Any?>) -> MultimapBuilder.SetMultimapBuilder<Any?, Any?> = { it.hashSetValues() },
    init: MultiMapBuilder<K, V, SetMultimap<K, V>>.() -> Unit
): SetMultimap<K, V> = MultiMapBuilder(values(keys).build<K, V>()).apply(init).build()

inline fun <K, V: Comparable<V>> sortedSetMultimap(
    keys: MultimapBuilder.MultimapBuilderWithKeys<Any?> = MultimapBuilder.linkedHashKeys(),
    init: MultiMapBuilder<K, V, SortedSetMultimap<K, V>>.() -> Unit
): SortedSetMultimap<K, V> = MultiMapBuilder(keys.treeSetValues().build<K, V>()).apply(init).build()