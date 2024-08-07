package net.essentuan.esl.json.type

import java.util.*
import java.util.function.Function
import java.util.function.Supplier
import kotlin.reflect.KClass

typealias AnyJson = JsonType<*, *, *>

/**
 * An interface for all types similar to Json
 */
interface JsonType<T : JsonType<T, V, E>, V : JsonType.Value, E : JsonType.Entry> {
    val size: Int

    fun isEmpty(): Boolean
    
    fun isNotEmpty(): Boolean = !isEmpty()

    operator fun set(key: String, value: Any?): V?

    fun add(key: String, value: Any?): T {
        return apply { this[key] = value } as T
    }

    fun addAll(json: AnyJson)

    operator fun plusAssign(json: T) = addAll(json)

    operator fun get(key: String): V?

    fun get(key: String, compute: (String) -> Any?): V

    operator fun contains(key: String): Boolean

    fun has(key: String): Boolean {
        return contains(key)
    }

    fun isNull(key: String): Boolean {
        return this[key]?.isNull() != false
    }

    fun typeOf(key: String): Class<*>? {
        return this[key]?.type
    }

    fun `is`(key: String, cls: Class<*>): Boolean {
        val type = typeOf(key) ?: return false;

        return type == cls || cls.isAssignableFrom(type)
    }

    fun copy(from: String, vararg to: String): T

    fun cut(from: String, vararg to: String): T

    fun delete(key: String): T

    fun remove(key: String): V?

    fun setAll(map: Map<String, Any?>): T

    fun deleteAll(vararg keys: String): T

    fun deleteAll(c: Collection<String>): T

    fun <T> deleteAll(c: Collection<T>, mapper: Function<T, String>): T

    @Throws(ClassCastException::class)
    fun <T> get(key: String, cls: Class<T>): T?

    @Throws(ClassCastException::class)
    fun <T : Any> get(key: String, cls: KClass<T>): T?

    @Throws(ClassCastException::class)
    fun <T : Any> get(key: String, default: T): T

    @Throws(ClassCastException::class)
    fun getNumber(key: String): Number?

    @Throws(ClassCastException::class)
    fun <T : Number?> getNumber(key: String, getter: Function<Number?, T?>): T? {
        val number = getNumber(key) ?: return null

        return getter.apply(number)
    }

    @Throws(ClassCastException::class)
    fun getInteger(key: String): Int?

    @Throws(ClassCastException::class)
    fun getInteger(key: String, default: Int): Int

    @Throws(ClassCastException::class)
    fun getLong(key: String): Long?

    @Throws(ClassCastException::class)
    fun getLong(key: String, default: Long): Long

    @Throws(ClassCastException::class)
    fun getFloat(key: String): Float?

    @Throws(ClassCastException::class)
    fun getFloat(key: String, default: Float): Float

    @Throws(ClassCastException::class)
    fun getDouble(key: String): Double?

    @Throws(ClassCastException::class)
    fun getDouble(key: String, default: Double): Double

    @Throws(ClassCastException::class)
    fun getString(key: String): String?

    @Throws(ClassCastException::class)
    fun getString(key: String, default: String): String

    @Throws(ClassCastException::class)
    fun getBoolean(key: String): Boolean?

    @Throws(ClassCastException::class)
    fun getBoolean(key: String, default: Boolean): Boolean

    @Throws(ClassCastException::class)
    fun getDate(key: String): Date?

    @Throws(ClassCastException::class)
    fun getDate(key: String, default: Date): Date

    @Throws(ClassCastException::class)
    fun getDate(key: String, default: Long): Date {
        return getDate(key, Date(default))
    }

    @Throws(ClassCastException::class)
    fun getList(key: String): MutableList<*>?

    @Throws(ClassCastException::class)
    fun getList(key: String, default: MutableList<*>): MutableList<*>

    @Throws(ClassCastException::class)
    fun <T> getList(key: String, cls: Class<T>): MutableList<T>?

    @Throws(ClassCastException::class)
    fun <T : Any> getList(key: String, cls: KClass<T>): MutableList<T>? {
        return getList(key, cls.java)
    }

    @Throws(ClassCastException::class)
    fun <T> getList(key: String, cls: Class<T>, default: MutableList<T>): MutableList<T>

    @Throws(ClassCastException::class)
    fun <T : Any> getList(key: String, cls: KClass<T>, default: MutableList<T>): MutableList<T> {
        return getList(key, cls.java, default)
    }

    fun isInt(key: String): Boolean {
        return this[key]?.isInt() == true
    }

    fun isLong(key: String): Boolean {
        return this[key]?.isLong() == true
    }

    fun isDouble(key: String): Boolean {
        return this[key]?.isDouble() == true
    }

    fun isBoolean(key: String): Boolean {
        return this[key]?.isBoolean() == true
    }

    fun isString(key: String): Boolean {
        return this[key]?.isString() == true
    }

    fun isDate(key: String): Boolean {
        return this[key]?.isDate() == true
    }

    fun isList(key: String): Boolean {
        return this[key]?.isList() == true
    }

    override fun toString(): String

    override fun hashCode(): Int

    override fun equals(other: Any?): Boolean

    val keys: MutableSet<String>
    
    val values: Values<V>
    
    val entries: MutableSet<E>

    fun asMap(): MutableMap<String, V>

    interface Value {
        val raw: Any?

        val type: Class<*>?
            get() = raw?.javaClass

        infix fun `is`(cls: Class<*>): Boolean {
            val type = type ?: return false;

            return type == cls || cls.isAssignableFrom(type)
        }

        infix fun `is`(cls: KClass<*>): Boolean = `is`(cls.java)

        @Throws(ClassCastException::class)
        infix fun <T> `as`(cls: Class<T>): T?

        @Throws(ClassCastException::class)
        infix fun <T : Any> `as`(cls: KClass<T>): T? {
            return `as`(cls.java)
        }

        @Throws(ClassCastException::class)
        infix fun <T : Any> `as`(default: T): T

        @Throws(ClassCastException::class)
        fun asNumber(): Number?

        @Throws(ClassCastException::class)
        fun <T : Number> asNumber(mapper: Function<Number, T?>): T? {
            return if (isNull()) null else mapper.apply(asNumber()!!);
        }

        @Throws(ClassCastException::class)
        fun <T : Number> asNumber(default: T, mapper: Function<Number, T?>): T {
            return if (isNull()) default else mapper.apply(asNumber()!!) ?: default
        }

        @Throws(ClassCastException::class)
        fun asInteger(): Int?

        @Throws(ClassCastException::class)
        fun asInteger(default: Int): Int

        @Throws(ClassCastException::class)
        fun asLong(): Long?

        @Throws(ClassCastException::class)
        fun asLong(default: Long): Long

        @Throws(ClassCastException::class)
        fun asFloat(): Float?

        @Throws(ClassCastException::class)
        fun asFloat(default: Float): Float

        @Throws(ClassCastException::class)
        fun asDouble(): Double?

        @Throws(ClassCastException::class)
        fun asDouble(default: Double): Double

        @Throws(ClassCastException::class)
        fun asString(): String?

        @Throws(ClassCastException::class)
        fun asString(default: String): String

        @Throws(ClassCastException::class)
        fun asBoolean(): Boolean?

        @Throws(ClassCastException::class)
        fun asBoolean(default: Boolean): Boolean

        @Throws(ClassCastException::class)
        fun asDate(): Date?

        @Throws(ClassCastException::class)
        fun asDate(default: Date): Date

        @Throws(ClassCastException::class)
        fun asDate(default: Long): Date {
            return asDate(Date(default))
        }

        @Throws(ClassCastException::class)
        fun asList(): MutableList<*>?

        @Throws(ClassCastException::class)
        fun asList(default: MutableList<*>): MutableList<*>

        @Throws(ClassCastException::class)
        fun <T> asList(cls: Class<T>): MutableList<T>?

        @Throws(ClassCastException::class)
        fun <T : Any> asList(cls: KClass<T>): MutableList<T>? {
            return asList(cls.java)
        }

        @Throws(ClassCastException::class)
        fun <T> asList(cls: Class<T>, default: MutableList<T>): MutableList<T>

        fun isNumber(): Boolean {
            return raw is Number
        }

        fun isInt(): Boolean {
            return raw is Int
        }

        fun isLong(): Boolean {
            return raw is Long
        }

        fun isDouble(): Boolean {
            return raw is Double
        }

        fun isBoolean(): Boolean {
            return raw is Boolean
        }

        fun isString(): Boolean {
            return raw is String
        }

        fun isDate(): Boolean {
            return raw is Date || raw is Long
        }

        fun isList(): Boolean {
            return raw is List<*>
        }

        fun isNull(): Boolean {
            return raw == null
        }
    }

    interface Values<V: Value> : MutableIterable<V> {
        val size: Int

        fun clear()

        fun isEmpty(): Boolean

        operator fun contains(element: Any?): Boolean

        fun remove(element: Any?): Boolean
    }

    interface Entry : Value {
        val key: String

        var value: Any?
    }

    interface Adapter<From, To : AnyJson> {
        fun from(): Class<From>

        fun to(): Class<To>

        fun convert(obj: From): To
    }

    companion object {
        inline fun <reified T : AnyJson> valueOf(obj: Any): T = valueOf(obj, T::class.java)

        fun <T : AnyJson> valueOf(obj: Any, cls: Class<T>) = Adapters.valueOf(obj, cls)

        fun register(vararg adapters: Adapter<*, *>) = adapters.forEach(Adapters::register)
    }
}

fun <T : AnyJson> jsonType(default: T, empty: Supplier<T>, init: AbstractBuilder<T, JsonType.Value, JsonType.Entry>.() -> Unit): T =
    AbstractBuilder.create(empty, default).apply(init).jsonType

fun <T : AnyJson> jsonType(empty: Supplier<T>, init: AbstractBuilder<T, JsonType.Value, JsonType.Entry>.() -> Unit): T = jsonType(empty.get(), empty, init)
