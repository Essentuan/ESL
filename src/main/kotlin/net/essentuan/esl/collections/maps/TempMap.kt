package net.essentuan.esl.collections.maps

import net.essentuan.esl.iteration.`break`
import net.essentuan.esl.iteration.extensions.mutable.iterate
import net.essentuan.esl.other.lock
import net.essentuan.esl.other.unsupported
import net.essentuan.esl.scheduling.annotations.Every
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.seconds
import net.essentuan.esl.time.extensions.timeSince
import java.util.Date

interface TempMap<K, V> : MutableMap<K, V> {
    /**
     * The mutex used when performing synchronous operations
     */
    val mutex: Any?

    /**
     * Clears all expired entries from the Map
     */
    fun cleanse()

    /**
     * Adds a listener that is called whenever an entry expires
     */
    fun onExpiry(listener: (Map.Entry<K, V>) -> Unit)
}

@Suppress("UNCHECKED_CAST")
fun <K, V> MutableMap<K, V>.expireAfter(expiry: (Pair<K, V>) -> Duration): TempMap<K, V> {
    return Impl(this as MutableMap<K, Impl<K, V>.Store>, expiry)
}

fun <K, V> MutableMap<K, V>.expireAfter(duration: Duration): TempMap<K, V> = expireAfter { duration }

private class Impl<K, V>(
    val backing: MutableMap<K, Impl<K, V>.Store>,
    val expiry: (Pair<K, V>) -> Duration,
) : TempMap<K, V> {
    val listeners = mutableListOf<(Map.Entry<K, V>) -> Unit>()

    override val mutex: Any
        get() = backing

    private fun expired(key: K, value: V) {
        expired(object : Map.Entry<K, V> {
            override val key: K
                get() = key
            override val value: V
                get() = value
        })
    }

    private fun expired(entry: Map.Entry<K, V>) {
        for (listener in listeners) {
            listener(entry)
        }
    }

    @Every(seconds = 10.0)
    override fun cleanse() {
        backing.lock {
            entries iterate {
                if (it.value.isExpired) {
                    remove()

                    expired(it.key, it.value.value)
                }
            }
        }
    }

    override fun onExpiry(listener: (Map.Entry<K, V>) -> Unit) {
        listeners += listener
    }

    override val size: Int
        get() {
            cleanse()

            return backing.size
        }

    override fun clear() =
        backing.clear()

    override fun isEmpty(): Boolean = size == 0

    override fun remove(key: K): V? {
        val store = backing.remove(key)

        return when {
            store == null -> null
            store.isExpired -> {
                expired(key, store.value)

                null
            }

            else -> store.value
        }
    }

    override fun putAll(from: Map<out K, V>) {
        for ((k, v) in from)
            this[k] = v
    }

    override fun put(key: K, value: V): V? =
        backing.put(key, Store(key, value))?.value

    override fun get(key: K): V? {
        val store = backing[key] ?: return null

        return if (store.isExpired) {
            remove(key)

            expired(key, store.value)

            null
        } else
            store.value
    }

    override fun containsValue(value: V): Boolean = value != null && values.contains(value)

    override fun containsKey(key: K): Boolean = backing[key]?.run {
        !isExpired && value != null
    } == true

    override val keys: MutableSet<K> by lazy { Keys() }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> by lazy { Entries() }

    override val values: MutableCollection<V> by lazy { Values() }

    inner class Entries : MutableSet<MutableMap.MutableEntry<K, V>> {
        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean =
            this@Impl.put(element.key, element.value) != null

        override val size: Int
            get() = this@Impl.size

        override fun clear() = this@Impl.clear()

        override fun isEmpty(): Boolean = this@Impl.isEmpty()

        override fun retainAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            var flag = false

            this iterate {
                if (it in elements) {
                    remove(it)

                    flag = true
                }
            }

            return flag
        }

        override fun removeAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            var flag = false

            for (element in elements)
                flag = flag or remove(element)

            return flag
        }

        override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
            return element in this && this@Impl.remove(element.key) != null
        }

        override fun containsAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            for (element in elements)
                if (element !in this)
                    return false

            return true
        }

        override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean {
            return get(element.key) == element.value
        }

        override fun addAll(elements: Collection<MutableMap.MutableEntry<K, V>>): Boolean {
            var flag = false

            for (element in elements)
                flag = flag or add(element)

            return flag
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> {
            return Iter(::Node)
        }

        inner class Node(val base: MutableMap.MutableEntry<K, Store>) : MutableMap.MutableEntry<K, V> {
            override val key: K
                get() = base.key
            override val value: V
                get() = base.value.value

            override fun setValue(newValue: V): V = base.setValue(Store(key, newValue)).value

            override fun equals(other: Any?): Boolean {
                return base == other
            }

            override fun hashCode(): Int {
                return base.hashCode()
            }

            override fun toString(): String {
                return "Map.Entry[key=$key, value=$value]"
            }
        }
    }

    inner class Keys : MutableSet<K> {
        override fun add(element: K): Boolean = unsupported()

        override val size: Int
            get() = this@Impl.size

        override fun clear() = this@Impl.clear()

        override fun isEmpty(): Boolean = this@Impl.isEmpty()

        override fun iterator(): MutableIterator<K> = Iter { it.key }

        override fun retainAll(elements: Collection<K>): Boolean {
            var flag = false

            this iterate {
                if (it in elements) {
                    remove(it)

                    flag = true
                }
            }

            return flag
        }

        override fun removeAll(elements: Collection<K>): Boolean {
            var flag = false

            for (element in elements)
                flag = flag or remove(element)

            return flag
        }

        override fun remove(element: K): Boolean {
            return this@Impl.remove(element) != null
        }

        override fun containsAll(elements: Collection<K>): Boolean {
            for (element in elements)
                if (element !in this)
                    return false

            return true
        }

        override fun contains(element: K): Boolean {
            return this@Impl.containsKey(element)
        }

        override fun addAll(elements: Collection<K>): Boolean = unsupported()
    }

    inner class Values : MutableCollection<V> {
        override val size: Int
            get() = this@Impl.size

        override fun clear() = this@Impl.clear()

        override fun isEmpty(): Boolean = this@Impl.isEmpty()

        override fun iterator(): MutableIterator<V> = Iter { it.value.value }

        override fun retainAll(elements: Collection<V>): Boolean {
            var flag = false

            this iterate {
                if (it in elements) {
                    remove(it)

                    flag = true
                }
            }

            return flag
        }

        override fun removeAll(elements: Collection<V>): Boolean {
            var flag = false

            for (element in elements)
                flag = flag or remove(element)

            return flag
        }

        override fun remove(element: V): Boolean {
            var flag = false

            this iterate {
                if (it == element) {
                    remove()

                    flag = true

                    `break`()
                }
            }

            return flag
        }

        override fun containsAll(elements: Collection<V>): Boolean {
            for (element in elements)
                if (element !in this)
                    return false

            return true
        }

        override fun contains(element: V): Boolean {
            return this@Impl.containsValue(element)
        }

        override fun addAll(elements: Collection<V>): Boolean = unsupported()

        override fun add(element: V): Boolean = unsupported()
    }

    inner class Store(key: K, val value: V) {
        val expiry = this@Impl.expiry(key to value)
        val created: Date = Date()

        val isExpired: Boolean
            get() = created.timeSince() > expiry

        override fun hashCode(): Int = value.hashCode()

        override fun equals(other: Any?): Boolean = value == other

        override fun toString(): String = value.toString()
    }

    private inner class Iter<T>(
        val mapper: (MutableMap.MutableEntry<K, Store>) -> T
    ) : MutableIterator<T> {
        val base by lazy { this@Impl.backing.iterator() }
        var value: MutableMap.MutableEntry<K, Store>? = null

        override fun hasNext(): Boolean {
            if (!base.hasNext())
                return false

            while (base.hasNext() && value == null) {
                val next = base.next()

                if (!next.value.isExpired)
                    value = next
            }

            return value != null
        }

        override fun next(): T {
            if (!hasNext())
                throw NoSuchElementException()

            return (value ?: throw NoSuchElementException()).let {
                value = null

                mapper(it)
            }
        }

        override fun remove() = base.remove()
    }
}