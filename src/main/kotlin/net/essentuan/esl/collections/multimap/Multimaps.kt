package net.essentuan.esl.collections.multimap

import com.google.common.collect.ListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Ordering
import com.google.common.collect.SetMultimap
import com.google.common.collect.SortedSetMultimap
import net.essentuan.esl.collections.mutableSetFrom
import net.essentuan.esl.collections.synchronized
import java.util.EnumMap
import java.util.EnumSet
import java.util.LinkedList
import java.util.SortedSet
import java.util.TreeMap
import java.util.TreeSet
import java.util.WeakHashMap
import kotlin.experimental.ExperimentalTypeInference

private typealias Factory = com.google.common.collect.Multimaps

object Multimaps {
    fun hashKeys(): MultiMapBuilder {
        return MultiMapBuilder { HashMap<Any?, Collection<Any?>>() }
    }

    fun hashKeys(expectedKeys: Int): MultiMapBuilder {
        require(expectedKeys >= 0) { "expectedKeys must be positive!" }

        return MultiMapBuilder { HashMap<Any?, Collection<Any?>>(expectedKeys) }
    }

    fun linkedHashKeys(expectedKeys: Int): MultiMapBuilder {
        require(expectedKeys >= 0) { "expectedKeys must be positive!" }

        return MultiMapBuilder { LinkedHashMap<Any?, Collection<Any?>>(expectedKeys) }
    }

    fun linkedHashKeys(): MultiMapBuilder {
        return MultiMapBuilder { LinkedHashMap<Any?, Collection<Any?>>() }
    }

    fun weakKeys(): MultiMapBuilder =
        MultiMapBuilder { WeakHashMap<Any?, Collection<Any?>>() }

    fun weakKeys(expectedKeys: Int): MultiMapBuilder =
        MultiMapBuilder { WeakHashMap<Any?, Collection<Any?>>(expectedKeys) }

    fun <T: Enum<T>> enumKeys(cls: Class<T>): MultiMapBuilder =
        MultiMapBuilder { EnumMap(cls) }

    inline fun <reified T: Enum<T>> enumKeys(): MultiMapBuilder =
        enumKeys(T::class.java)

    fun <T> treeKeys(comparator: Comparator<T>): MultiMapBuilder =
        MultiMapBuilder { TreeMap(comparator) }

    fun treeKeys(): MultiMapBuilder =
        MultiMapBuilder { TreeMap(Ordering.natural()) }

    inline fun keys(crossinline block: () -> MutableMap<in Any?, Collection<Any?>>) =
        MultiMapBuilder { block() }
}

fun interface MultiMapBuilder {
    fun create(): MutableMap<*, Collection<Any?>>
}

fun MultiMapBuilder.synchronized(): MultiMapBuilder =
    MultiMapBuilder { this@synchronized.create().synchronized() }

@Suppress("UNCHECKED_CAST")
operator fun <K, V> MultiMapBuilder.invoke(): MutableMap<K, Collection<V>> =
    create() as MutableMap<K, Collection<V>>

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <K, V> MultiMapBuilder.values(crossinline block: () -> Collection<V>): Multimap<K, V> =
    Factory.newMultimap(this()) { block() }

@JvmName("list")
inline fun <K, V> MultiMapBuilder.values(crossinline block: () -> List<V>): ListMultimap<K, V> =
    Factory.newListMultimap(this()) { block() }

@JvmName("set")
inline fun <K, V> MultiMapBuilder.values(crossinline block: () -> Set<V>): SetMultimap<K, V> =
    Factory.newSetMultimap(this()) { block() }

@JvmName("sortedSet")
inline fun <K, V> MultiMapBuilder.values(crossinline block: () -> SortedSet<V>): SortedSetMultimap<K, V> =
    Factory.newSortedSetMultimap(this()) { block() }

fun <K, V> MultiMapBuilder.arrayListValues(): ListMultimap<K, V> =
    Factory.newListMultimap<K, V>(this(), ::ArrayList)

fun <K, V> MultiMapBuilder.arrayListValues(capacity: Int): ListMultimap<K, V> =
    Factory.newListMultimap<K, V>(this()) { ArrayList(capacity) }

fun <K, V> MultiMapBuilder.linkedListValues(): ListMultimap<K, V> =
    Factory.newListMultimap<K, V>(this(), ::LinkedList)

fun <K, V> MultiMapBuilder.hashSetValues(): SetMultimap<K, V> =
    Factory.newSetMultimap<K, V>(this(), ::HashSet)

fun <K, V> MultiMapBuilder.hashSetValues(numElements: Int): SetMultimap<K, V> =
    Factory.newSetMultimap<K, V>(this()) { HashSet(numElements) }

fun <K, V> MultiMapBuilder.linkedHashSetValues(): SetMultimap<K, V> =
    Factory.newSetMultimap<K, V>(this(), ::LinkedHashSet)

fun <K, V> MultiMapBuilder.linkedHashSetValues(numElements: Int): SetMultimap<K, V> =
    Factory.newSetMultimap<K, V>(this()) { LinkedHashSet(numElements) }

fun <K, V: Enum<V>> MultiMapBuilder.enumSetValues(cls: Class<V>): SetMultimap<K, V> =
    Factory.newSetMultimap<K, V>(this()) { EnumSet.noneOf(cls) }

inline fun <K, reified V: Enum<V>> MultiMapBuilder.enumSetValues(): SetMultimap<K, V> =
    enumSetValues(V::class.java)

fun <K, V> MultiMapBuilder.treeSetValues(comparator: Comparator<V>): SetMultimap<K, V> =
    Factory.newSetMultimap<K, V>(this()) { TreeSet(comparator) }

fun <K, V: Comparable<V>> MultiMapBuilder.treeSetValues(): SetMultimap<K, V> =
    Factory.newSetMultimap<K, V>(this()) { TreeSet(naturalOrder<V>()) }

fun <K, V> MultiMapBuilder.weakValues(): SetMultimap<K, V> =
    Factory.newSetMultimap<K, V>(this()) { mutableSetFrom(::WeakHashMap) }