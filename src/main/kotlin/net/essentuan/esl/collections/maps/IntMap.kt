package net.essentuan.esl.collections.maps

import java.util.AbstractMap
import java.util.function.BiConsumer
import java.util.function.BiFunction
import java.util.function.Function
import kotlin.math.max

private const val SOFT_MAX_ARRAY_LENGTH = Int.MAX_VALUE - 8;

open class IntMap<T>(
    capacity: Int = 8,
    private var min: Int = 0,
) : AbstractMap<Int, T>() {
    final override var size = 0
        private set

    protected var backing = arrayOfNulls<Node>(capacity)
        private set
    protected var modCount: Int = 0
        private set

    private fun newLength(oldLength: Int, minGrowth: Int, prefGrowth: Int): Int {
        val prefLength =
            (oldLength + max(minGrowth.toDouble(), prefGrowth.toDouble())).toInt()
        return if (prefLength in 1..SOFT_MAX_ARRAY_LENGTH) {
            prefLength
        } else {
            hugeLength(oldLength, minGrowth)
        }
    }

    private fun hugeLength(oldLength: Int, minGrowth: Int): Int {
        val minLength = oldLength + minGrowth
        return if (minLength < 0) {
            throw OutOfMemoryError(
                "Required array length $oldLength + $minGrowth is too large"
            )
        } else if (minLength <= SOFT_MAX_ARRAY_LENGTH) {
            SOFT_MAX_ARRAY_LENGTH
        } else {
            minLength
        }
    }

    /**
     * Resizes the backing array to include both min and max
     *
     * @param min The new minimum key
     * @param max The new maximum key
     */
    private fun resize(min: Int, max: Int) {
        val size = max - min
        val oldCapacity = backing.size

        if (size < oldCapacity)
            return

        val capacity: Int = newLength(
            oldCapacity,
            (size + 1) - oldCapacity,
            oldCapacity shr 1
        )

        val new = arrayOfNulls<Node>(capacity)
        System.arraycopy(backing, 0, new, this.min - min, oldCapacity)

        backing = new
        this.min = min

        modCount++
    }

    private fun at(int: Int): Node? = when {
        int >= 0 && int < backing.size -> backing[int]
        else -> null
    }

    private inline fun <U> set(key: Int, value: T, block: (Node?) -> U): U {
        if (key >= backing.size)
            resize(min, key)
        else if (key < min)
            resize(key, backing.size - 1)

        val index = key - min
        val node = backing[index]

        if (node != null) {
            return block(node).also {
                node.value = value
            }
        }

        backing[index] = Node(key, value)
        size++
        modCount++

        return block(null)
    }

    override fun isEmpty(): Boolean =
        size == 0

    override fun clear() {
        for (i in backing.indices)
            backing[i] = null

        size = 0
        modCount++
    }

    override fun containsKey(key: Int?): Boolean {
        return at((key ?: return false) - min) != null
    }

    override fun containsValue(value: T): Boolean {
        for (node in backing) {
            if (node != null && node.value == value)
                return true
        }

        return false
    }

    override fun get(key: Int?): T? {
        return at((key ?: return null) - min)?.value
    }

    override fun put(key: Int?, value: T): T? {
        return set(key ?: return null, value) { it?.value }
    }

    override fun remove(key: Int?): T? {
        val index = (key ?: return null) - min

        if (index < 0 || index >= backing.size)
            return null

        val node = backing[index]
        backing[index] = null

        size--
        modCount++

        return node?.value
    }

    private inline fun <U> safe(expected: Int = modCount, block: () -> U): U {
        val res = block()

        if (expected != modCount)
            throw ConcurrentModificationException()

        return res
    }

    private inline fun <U> mutate(mod: Int = modCount, block: (inc: () -> Unit) -> U): U {
        var expected: Int = mod
        val res = block {
            expected++
            modCount++
        }

        if (expected != modCount)
            throw ConcurrentModificationException()

        return res
    }

    override fun compute(key: Int?, remappingFunction: BiFunction<in Int, in T?, out T?>): T? {
        val index = (key ?: return null) - min

        return if (index < 0 || index >= backing.size)
            (remappingFunction.apply(key, null) ?: return null).also { put(key, it) }
        else mutate { inc ->
            val node = backing[index]

            if (node == null)
                (remappingFunction.apply(key, null) ?: return@mutate null).also {
                    backing[index] = Node(key, it)
                    size++

                    inc()
                }
            else
                remappingFunction.apply(key, node.value).also {
                    if (it == null) {
                        backing[index] = null
                        size--

                        inc()
                    } else {
                        node.value = it

                        inc()
                    }
                }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun computeIfAbsent(key: Int?, mappingFunction: Function<in Int, out T>): T {
        val index = (key ?: return null as T) - min

        return if (index < 0 || index >= backing.size)
            mappingFunction.apply(key).also { put(key, it) }
        else mutate { inc ->
            val node = backing[index]

            if (node == null)
                mappingFunction.apply(key).also {
                    backing[index] = Node(key, it)
                    size++

                    inc()
                }
            else
                node.value
        }
    }

    override fun computeIfPresent(key: Int?, remappingFunction: BiFunction<in Int, in T & Any, out T?>): T? {
        val index = (key ?: return null) - min

        return if (index < 0 || index >= backing.size)
            null
        else mutate { inc ->
            val node = backing[index] ?: return@mutate null

            remappingFunction.apply(key, node.value ?: return@mutate null).also {
                if (it == null) {
                    backing[index] = null
                    size--

                    inc()
                } else {
                    node.value = it
                    inc()
                }
            }
        }
    }

    override fun merge(key: Int?, value: T & Any, remappingFunction: BiFunction<in T & Any, in T & Any, out T?>): T? {
        val index = (key ?: return null) - min

        return if (index < 0 || index >= backing.size) {
            put(key, value)
            return value
        } else mutate { inc ->
            val node = backing[index]

            if (node == null) {
                backing[index] = Node(key, value)
                size++

                inc()

                value
            } else if (node.value == null) {
                node.value = value

                inc()

                value
            } else {
                remappingFunction.apply(node.value ?: throw ConcurrentModificationException(), value).also {
                    if (it == null)
                        backing[index] = null
                    else
                        node.value = value

                    inc()
                }
            }
        }
    }

    override fun forEach(action: BiConsumer<in Int, in T>) = safe {
        for (node in backing)
            if (node != null)
                action.accept(node.key, node.value)
    }

    private lateinit var _entries: Entries

    override val entries: MutableSet<MutableMap.MutableEntry<Int, T>>
        get() {
            if (!::_entries.isInitialized)
                _entries = Entries()

            return _entries
        }

    protected inner class Node(
        override val key: Int,
        override var value: T
    ) : MutableMap.MutableEntry<Int, T> {
        override fun setValue(newValue: T): T =
            value.also { value = newValue }

        override fun equals(other: Any?): Boolean {
            return this === other || (
                    other is MutableMap.MutableEntry<*, *>
                            && key == other.key
                            && other.value == value
                    )
        }

        override fun hashCode(): Int {
            return key xor (value?.hashCode() ?: 0)
        }

        override fun toString(): String {
            return "$key=$value"
        }
    }

    private inner class Iterator : MutableIterator<MutableMap.MutableEntry<Int, T>> {
        var expected: Int = modCount
        var next: Int = 0
        var current: Int = -1

        override fun hasNext(): Boolean = safe(expected) {
            if (size == 0)
                return false

            while (next < backing.size && backing[next] == null)
                next++

            return next < backing.size && backing[next] != null
        }

        override fun next(): MutableMap.MutableEntry<Int, T> {
            if (next >= backing.size)
                throw NoSuchElementException()

            if (expected != modCount)
                throw ConcurrentModificationException()

            return backing[next]?.also {
                current = next++
                hasNext()
            } ?: throw IllegalStateException()
        }

        override fun remove() {
            if (current == -1)
                throw IllegalStateException()

            if (expected != modCount)
                throw ConcurrentModificationException()

            if (backing[current] != null) {
                backing[current] = null

                expected++
                modCount++
            }

            current = -1
        }
    }

    private inner class Entries : AbstractMutableSet<MutableMap.MutableEntry<Int, T>>() {
        override val size: Int
            get() = this@IntMap.size

        override fun isEmpty(): Boolean = this@IntMap.isEmpty()

        override fun clear() {
            this@IntMap.clear()
        }

        override fun add(element: MutableMap.MutableEntry<Int, T>): Boolean {
            val value = element.value

            return set(element.key, value) { it != null && it.value == value }
        }

        override fun contains(element: MutableMap.MutableEntry<Int, T>): Boolean =
            this@IntMap.at(element.key - min)?.value == element.value

        override fun remove(element: MutableMap.MutableEntry<Int, T>): Boolean =
            this@IntMap.remove(element.key, element.value)

        override fun iterator(): MutableIterator<MutableMap.MutableEntry<Int, T>> =
            Iterator()
    }


}