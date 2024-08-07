package net.essentuan.esl.collections.maps

import net.essentuan.esl.comparing.equals
import net.essentuan.esl.other.unsupported
import java.util.Objects

abstract class Registry<K : Any, V>(
    private val backing: MutableMap<K, Registry<K, V>.Entry>
) : AbstractMutableMap<K, V>() {
    private var mod = 0L

    protected abstract fun trace(key: K): Sequence<K>

    fun link() {
        backing.values.forEach { it.backing }
    }

    override val size: Int
        get() = backing.size

    override fun clear() = backing.clear()

    override fun isEmpty(): Boolean = backing.isEmpty()

    override fun remove(key: K): V? =
        backing.remove(key)?.also {
            mod++
        }?.value

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from)
            put(key, value)
    }

    override fun put(key: K, value: V): V? {
        var old: V? = null

        backing.compute(key) { _, entry ->
            entry?.also {
                old = it.backing
                it.backing = value
            } ?: Entry(key, value)
        }

        mod++

        return old
    }

    protected fun probe(key: K): Entry? = backing[key] ?: run {
        for (k in trace(key)) {
            val entry = backing[k]

            if (entry != null) {
                return Entry(key, entry).also {
                    backing[key] = it
                    mod++
                }
            }
        }

        return null
    }

    override fun get(key: K): V? =
        probe(key)?.backing

    override fun containsValue(value: V): Boolean =
        value in values

    override fun containsKey(key: K): Boolean =
        probe(key) != null

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> = Entries()
    override val keys: MutableSet<K> = Keys()
    override val values: MutableCollection<V> = Values()

    inner class Entry private constructor(
        override val key: K
    ) : MutableMap.MutableEntry<K, V> {
        internal constructor(key: K, pointer: Entry) : this(key) {
            this.pointer = pointer
        }

        internal constructor(key: K, value: V) : this(key) {
            this.backing = value
        }

        var hasValue = false

        var mod: Long = -1L
        var pointer: Entry? = null
            get() {
                if (hasValue)
                    return null

                if (field != null && mod == this@Registry.mod)
                    return field

                for (k in trace(key)) {
                    if (k == key)
                        continue

                    val entry = this@Registry.backing[k]

                    if (entry != null) {
                        field = entry
                        mod = this@Registry.mod

                        return field
                    }
                }

                mod = -1
                field = null

                return null
            }
            set(value) {
                mod = this@Registry.mod
                field = value
            }

        var backing: V? = null
            get() {
                if (hasValue)
                    return field

                return (pointer ?: return field).backing
            }
            set(value) {
                field = value
                mod = this@Registry.mod
                pointer = null
                hasValue = true
            }

        @Suppress("UNCHECKED_CAST")
        override val value: V
            get() = backing as V

        override fun setValue(newValue: V): V = value.also {
            backing = newValue
        }

        override fun hashCode(): Int = Objects.hash(key, backing)

        override fun equals(other: Any?): Boolean = equals<MutableMap.MutableEntry<K, V>>(other) { it, other ->
            it.key == other.key && it.value == other.value
        }

        override fun toString(): String = "$key=$value"
    }

    private abstract inner class Iterator<T> : MutableIterator<T> {
        private val iter = backing.iterator()
        protected abstract fun map(entry: Entry): T

        init { hasNext() }

        private var next: Entry? = null

        final override fun hasNext(): Boolean {
            if (next != null)
                return true

            if (!iter.hasNext())
                return false

            for ((_, entry) in iter) {
                if (entry.hasValue || entry.pointer != null) {
                    next = entry
                    break
                }
            }

            return next != null
        }

        override fun next(): T {
            if (!hasNext())
                throw NoSuchElementException()

            return map(next?.also { next = null } ?: throw NoSuchElementException())
        }

        override fun remove() = iter.remove()
    }

    private inner class Entries : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        override val size: Int
            get() = this@Registry.size

        override fun clear() {
            this@Registry.clear()
        }

        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean {
            return put(element.key, element.value) == element.value
        }

        override fun contains(element: MutableMap.MutableEntry<K, V>): Boolean {
            return get(element.key) == element.value
        }

        override fun remove(element: MutableMap.MutableEntry<K, V>): Boolean {
            return super.remove(element)
        }

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = object : Iterator<MutableMap.MutableEntry<K, V>>() {
            override fun map(entry: Registry<K, V>.Entry): MutableMap.MutableEntry<K, V> = entry
        }
    }

    private inner class Keys : AbstractMutableSet<K>() {
        override val size: Int
            get() = this@Registry.size

        override fun clear() {
            this@Registry.clear()
        }

        override fun add(element: K): Boolean = unsupported()

        override fun contains(element: K): Boolean = this@Registry.containsKey(element)

        override fun remove(element: K): Boolean =
            backing.remove(element)?.also {
                mod++
            } != null

        override fun iterator(): MutableIterator<K> = object : Iterator<K>() {
            override fun map(entry: Registry<K, V>.Entry): K = entry.key
        }
    }

    private inner class Values : AbstractMutableCollection<V>() {
        override val size: Int
            get() = this@Registry.size

        override fun clear() {
            this@Registry.clear()
        }

        override fun add(element: V): Boolean = unsupported()

        override fun iterator(): MutableIterator<V> = object : Iterator<V>() {
            override fun map(entry: Registry<K, V>.Entry): V = entry.value
        }
    }
}

inline fun <K : Any, V> registry(
    backing: MutableMap<K, Registry<K, V>.Entry> = mutableMapOf(),
    crossinline tracer: (K) -> Sequence<K>
) = object : Registry<K, V>(backing) {
    override fun trace(key: K): Sequence<K> = tracer(key)
}
