package net.essentuan.esl.json.type

import net.essentuan.esl.iteration.`break`
import net.essentuan.esl.iteration.extensions.map
import net.essentuan.esl.iteration.extensions.mutable.iterate
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.time.extensions.toDate
import java.util.Date
import java.util.Objects
import java.util.function.Function
import kotlin.reflect.KClass

abstract class MapBasedJsonType<T : JsonType<T, V, E>, V : JsonType.Value, E : JsonType.Entry>(
    val map: MutableMap<String, V> = LinkedHashMap()
) : JsonType<T, V, E> {
    abstract fun Any?.checkThis(message: (String) -> String): T

    inline fun <U : Any> lookup(key: String, create: Boolean = false, finisher: T.(String) -> U?): U? {
        @Suppress("UNCHECKED_CAST")
        var current: T = this as T
        var start = 0

        for (i in key.indices) {
            if (key[i] != '.')
                continue

            val part = key.substring(start, i)
            start = i + 1

            val next = if (!create)
                current.asMap()[part]
            else
                current.asMap().computeIfAbsent(part) { valueOf(empty()) }

            when (val value = next?.raw) {
                null -> return null
                else -> current = value.checkThis {
                    "Key $part is ${value.javaClass.simpleString()} not $it!"
                }
            }
        }

        return current.finisher(if (start == 0) key else key.substring(start))
    }


    override val size: Int
        get() = map.size

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun set(key: String, value: Any?): V? =
        lookup(key, true) { asMap().put(it, valueOf(value)) }

    override fun addAll(json: AnyJson) {
        map.putAll(json.checkThis {
            "Json is ${json.javaClass.simpleString()} not $it!"
        }.asMap())
    }

    override fun get(key: String): V? =
        lookup(key, false) { asMap()[it] }

    override fun get(key: String, compute: (String) -> Any?): V =
        lookup(key, true) {
            asMap().computeIfAbsent(it) {
                valueOf(compute(key).run {
                    if (this is JsonType.Value)
                        raw
                    else
                        this
                })
            }
        }!!

    override fun <T> get(key: String, cls: Class<T>): T? = get(key)?.`as`(cls)

    override fun <T : Any> get(key: String, cls: KClass<T>): T? = get(key)?.`as`(cls)

    override fun <T : Any> get(key: String, default: T): T = get(key)?.`as`(default::class) ?: default

    override fun contains(key: String): Boolean =
        lookup(key, false) { it in asMap() } == true

    override fun remove(key: String): V? =
        lookup(key, false) { asMap().remove(it) }

    @Suppress("UNCHECKED_CAST")
    override fun copy(from: String, vararg to: String): T {
        val value = this[from]?.raw

        for (key in to)
            this[key] = value

        return this as T
    }

    override fun cut(from: String, vararg to: String): T {
        val value = remove(from)?.raw

        for (key in to)
            this[key] = value

        return this as T
    }

    override fun delete(key: String): T =
        apply { remove(key) } as T

    override fun setAll(map: Map<String, Any?>): T =
        apply { map.forEach { (str, raw) -> this[str] = raw } } as T

    override fun deleteAll(vararg keys: String): T =
        apply { keys.forEach { key -> remove(key) } } as T

    override fun deleteAll(c: Collection<String>): T =
        apply { c.forEach { key -> remove(key) } } as T

    override fun <T> deleteAll(c: Collection<T>, mapper: Function<T, String>): T =
        apply { c.forEach { o -> remove(mapper.apply(o)) } } as T

    override fun getNumber(key: String): Number? = get(key)?.asNumber()

    override fun getInteger(key: String): Int? = get(key)?.asInteger()

    override fun getInteger(key: String, default: Int): Int = get(key)?.asInteger() ?: default

    override fun getLong(key: String): Long? = get(key)?.asLong()

    override fun getLong(key: String, default: Long): Long = get(key)?.asLong() ?: default

    override fun getFloat(key: String): Float? = get(key)?.asFloat()

    override fun getFloat(key: String, default: Float): Float = get(key)?.asFloat(default) ?: default

    override fun getDouble(key: String): Double? = get(key)?.asDouble()

    override fun getDouble(key: String, default: Double): Double = get(key)?.asDouble(default) ?: default

    override fun getString(key: String): String? = get(key)?.asString()

    override fun getString(key: String, default: String): String = get(key)?.asString(default) ?: default

    override fun getBoolean(key: String): Boolean? = get(key)?.asBoolean()

    override fun getBoolean(key: String, default: Boolean): Boolean = get(key)?.asBoolean(default) ?: default

    override fun getDate(key: String): Date? = get(key)?.asDate()

    override fun getDate(key: String, default: Date): Date = get(key)?.asDate(default) ?: default

    override fun getList(key: String): MutableList<*>? = get(key)?.asList()

    override fun getList(key: String, default: MutableList<*>): MutableList<*> = get(key)?.asList(default) ?: default

    override fun <T> getList(key: String, cls: Class<T>): MutableList<T>? = get(key)?.asList(cls)

    override fun <T> getList(key: String, cls: Class<T>, default: MutableList<T>): MutableList<T> =
        get(key)?.asList(cls, default) ?: default

    abstract fun empty(): T

    abstract fun valueOf(raw: Any?): V

    abstract fun entryOf(key: String, value: V): E

    fun entryFor(key: String): E? {
        val value = map[key] ?: return null

        return entryOf(key, value)
    }

    override val keys: MutableSet<String>
        get() = map.keys

    override val entries: MutableSet<E> by lazy {
        Entries(map.entries)
    }

    override val values: JsonType.Values<V> by lazy {
        Values(map.values)
    }

    override fun asMap(): MutableMap<String, V> = map

    open class Value(override val raw: Any?) : JsonType.Value {
        override fun <T> `as`(cls: Class<T>): T? = cls.cast(raw)

        override fun <T : Any> `as`(default: T): T = default.javaClass.cast(raw) ?: default

        override fun asNumber(): Number? = `as`(Number::class)

        override fun asInteger(): Int? = asNumber(Number::toInt)

        override fun asInteger(default: Int): Int = asNumber(default, Number::toInt)

        override fun asLong(): Long? = asNumber(Number::toLong)

        override fun asLong(default: Long): Long = asNumber(default, Number::toLong)

        override fun asFloat(): Float? = asNumber(Number::toFloat)

        override fun asFloat(default: Float): Float = asNumber(default, Number::toFloat)

        override fun asDouble(): Double? = asNumber(Number::toDouble)

        override fun asDouble(default: Double): Double = asNumber(default, Number::toDouble)

        override fun asString(): String? = raw?.toString()

        override fun asString(default: String): String = asString() ?: default

        override fun asBoolean(): Boolean? = `as`(Boolean::class)

        override fun asBoolean(default: Boolean): Boolean = asBoolean() ?: default

        override fun asDate(): Date? = when (val obj = raw) {
            is Date -> obj
            is Number -> Date(obj.toLong())
            is String -> obj.toDate()
            null -> null
            else -> throw ClassCastException("Cannot cast ${obj.javaClass.simpleString()} to Date!")
        }

        override fun asDate(default: Date): Date = asDate() ?: default

        @Suppress("UNCHECKED_CAST")
        protected fun <T> constructList(cls: Class<T>, defaultValue: MutableList<T>?): MutableList<T>? {
            val value: MutableList<*> = `as`(MutableList::class) ?: return defaultValue

            for (item in value) {
                if (item != null && !cls.isAssignableFrom(item.javaClass)) {
                    throw ClassCastException("List element cannot be cast from ${item.javaClass.simpleString()} to ${cls.simpleString()}!")
                }
            }

            return value as MutableList<T>
        }

        override fun asList(): MutableList<*>? = `as`(MutableList::class) as MutableList<*>

        override fun asList(default: MutableList<*>): MutableList<*> = asList() ?: default

        override fun <T> asList(cls: Class<T>): MutableList<T>? = constructList(cls, null)

        override fun <T> asList(cls: Class<T>, default: MutableList<T>): MutableList<T> = constructList(cls, default)!!

        override fun hashCode(): Int = Objects.hash(raw)

        override fun equals(other: Any?): Boolean = if (other is JsonType.Value) other.raw == raw else other == raw

        override fun toString(): String = raw.toString()
    }

    open inner class Entry<V : JsonType.Value>(override val key: String, open val obj: V) : JsonType.Entry,
        JsonType.Value by obj {
        override var value: Any?
            get() = obj.raw
            set(value) {
                map[key] = valueOf(value)
            }

        override fun hashCode(): Int = Objects.hash(key, obj)

        override fun equals(other: Any?): Boolean {
            if (other !is JsonType.Entry)
                return false

            return this === other || (key == other.key && obj == other.raw)
        }
    }

    open inner class Values(val values: MutableCollection<V>) : JsonType.Values<V> {
        override val size: Int
            get() = values.size

        override fun clear() = values.clear()

        override fun isEmpty(): Boolean = values.isEmpty()

        override fun iterator(): MutableIterator<V> = values.iterator()

        override fun contains(element: Any?): Boolean {
            for (obj in this)
                if (obj.raw == element)
                    return true

            return false
        }

        override fun remove(element: Any?): Boolean {
            var flag = false

            this iterate {
                if (it.raw == element) {
                    flag = true

                    remove()
                    `break`()
                }
            }

            return flag
        }
    }

    open inner class Entries(val entries: MutableSet<MutableMap.MutableEntry<String, V>>) : MutableSet<E> {
        override val size: Int
            get() = entries.size

        override fun add(element: E): Boolean =
            this@MapBasedJsonType.set(element.key, element.raw)?.raw == element.raw

        override fun clear() = entries.clear()

        override fun isEmpty(): Boolean = entries.isEmpty()

        override fun iterator(): MutableIterator<E> = entries.iterator()
            .map { entry -> entryOf(entry.key, entry.value) }

        override fun retainAll(elements: Collection<E>): Boolean {
            var mod = false

            iterator().run {
                while (hasNext())
                    if (next() !in elements) {
                        remove()
                        mod = true
                    }
            }

            return mod
        }

        override fun removeAll(elements: Collection<E>): Boolean {
            var mod = false

            for (e in elements)
                mod = remove(e) or mod

            return mod
        }

        override fun remove(element: E): Boolean = this@MapBasedJsonType.remove(element.key) != null

        override fun containsAll(elements: Collection<E>): Boolean {
            for (e in elements)
                if (e !in this)
                    return false

            return true
        }

        override fun contains(element: E): Boolean = this@MapBasedJsonType[element.key]?.raw == element.raw

        override fun addAll(elements: Collection<E>): Boolean {
            var mod = false

            for (e in elements)
                mod = add(e) or mod

            return mod
        }

    }
}