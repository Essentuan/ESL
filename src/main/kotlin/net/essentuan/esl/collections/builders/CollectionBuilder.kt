package net.essentuan.esl.collections.builders

import net.essentuan.esl.iteration.extensions.iterable
import java.util.stream.Stream

@JvmInline
value class CollectionBuilder<T, COLL: MutableCollection<T>>(private val collection: COLL) {
    operator fun T.unaryPlus() = collection.add(this)

    operator fun Iterable<T>.unaryPlus() = collection.addAll(this)

    operator fun Stream<T>.unaryPlus() = collection.addAll(this.iterable())

    operator fun Sequence<T>.unaryPlus() = collection.addAll(this)

    fun T.push() = collection.add(this)

    fun Iterable<T>.push() = collection.addAll(this)

    fun Stream<T>.push() = collection.addAll(this.iterable())

    fun Sequence<T>.push() = collection.addAll(this)

    fun build(): COLL = collection
}

inline fun <T> list(list: MutableList<T> = ArrayList(), init: CollectionBuilder<T, MutableList<T>>.() -> Unit): List<T> {
    return CollectionBuilder(list).apply(init).build()
}

inline fun <T> mutableList(list: MutableList<T> = ArrayList(), init: CollectionBuilder<T, MutableList<T>>.() -> Unit): MutableList<T> {
    return CollectionBuilder(list).apply(init).build()
}

inline fun <T> set(set: MutableSet<T> = LinkedHashSet(), init: CollectionBuilder<T, MutableSet<T>>.() -> Unit): Set<T> {
    return CollectionBuilder(set).apply(init).build()
}

inline fun <T> mutableSet(set: MutableSet<T> = LinkedHashSet(), init: CollectionBuilder<T, MutableSet<T>>.() -> Unit): MutableSet<T> {
    return CollectionBuilder(set).apply(init).build()
}

