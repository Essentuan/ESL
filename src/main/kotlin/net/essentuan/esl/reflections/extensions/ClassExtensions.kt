package net.essentuan.esl.reflections.extensions

import com.google.common.collect.Multimap
import net.essentuan.esl.collections.multimap.Multimaps
import net.essentuan.esl.collections.multimap.arrayListValues
import net.essentuan.esl.filter
import net.essentuan.esl.filterNotNull
import net.essentuan.esl.iteration.extensions.iterate
import net.essentuan.esl.map
import net.essentuan.esl.ofNullable
import net.essentuan.esl.orElse
import net.essentuan.esl.orNull
import net.essentuan.esl.raise
import net.essentuan.esl.unsafe
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable
import java.util.LinkedList
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance

private val constructorCache = HashMap<Class<*>, HashMap<Array<*>, Constructor<*>?>>()
private val methodCache = HashMap<Class<*>, HashMap<Array<*>, Method?>>()
private val fieldCache = HashMap<Class<*>, HashMap<String, Field?>>()

internal fun constructors(cls: Class<*>): HashMap<Array<*>, Constructor<*>?> =
    constructorCache.computeIfAbsent(cls) { HashMap() }

internal fun methods(cls: Class<*>): HashMap<Array<*>, Method?> = methodCache.computeIfAbsent(cls) { HashMap() }

internal fun fields(cls: Class<*>): HashMap<String, Field?> = fieldCache.computeIfAbsent(cls) { HashMap() }

@Suppress("UNCHECKED_CAST")
fun <T> Class<T>.constructor(vararg args: Class<*>): Constructor<T>? {
    return unsafe {
        constructors(this).computeIfAbsent(args) {
            unsafe {
                getConstructor(*args)
            }.orNull()
        } as Constructor<T>?
    }.orElse(null)
}

fun <T> Class<T>.hasConstructor(vararg args: Class<*>): Boolean = constructor(*args) != null

/**
 * Constructs a new object
 */
@Suppress("CheckedExceptionsKotlin")
fun <T> Class<T>.construct(vararg args: Any): T? {
    return unsafe { constructor(*args.map { e -> e.javaClass }.toTypedArray()) }.filterNotNull()
        .filter(Constructor<T>::trySetAccessible)
        .map { it.newInstance(*args) }
        .raise()
        .orElse(null)
}

fun <T> Class<T>.hasMethod(name: String, vararg args: Class<*>): Boolean = method(name, *args) != null

fun <T> Class<T>.method(name: String, vararg args: Class<*>): Method? {
    return unsafe {
        methods(this).computeIfAbsent(args) {
            unsafe {
                getMethod(name, *args)
            }.orNull()
        }
    }.orElse(null)
}

/**
 * Invokes a static method
 */
@Suppress("CheckedExceptionsKotlin")
fun <T> Class<T>.invoke(name: String, vararg args: Any): Any? {
    return unsafe { method(name, *args.map { e -> e.javaClass }.toTypedArray()) }
        .filterNotNull()
        .filter(Method::trySetAccessible)
        .map { it.invoke(this, *args) }
        .raise()
        .orNull()
}

internal fun <T> Class<T>.getFieldUID(name: String): String {
    return "${descriptorString()}+$name"
}

fun <T> Class<T>.field(name: String): Field? {
    val fieldUID = getFieldUID(name);

    return unsafe {
        fields(this).computeIfAbsent(fieldUID) {
            unsafe {
                getField(name)
            }.orNull()
        }
    }.orElse(null)
}

fun <T> Class<T>.hasField(name: String): Boolean = field(name) != null

/**
 * Sets a static field
 */
fun <T> Class<T>.set(field: String, value: Any) {
    field(field)
        .ofNullable()
        .filter(Field::isStatic)
        .filter(Field::trySetAccessible)
        .map { it.set(null, value) }
        .raise()
}

/**
 * Gets a static field
 */
@Suppress("CheckedExceptionsKotlin", "UNCHECKED_CAST")
fun <T, U> Class<T>.get(field: String, value: Any): U? {
    return field(field)
        .ofNullable()
        .filter(Field::isStatic)
        .filter(Field::trySetAccessible)
        .map { it.get(null) }
        .raise()
        .orNull() as U?
}

fun <T> Class<T>.fullString(): String {
    val builder = StringBuilder()

    var enclosing: Class<*>? = this
    while (enclosing != null) {
        builder.insert(0, enclosing.simpleName)
        enclosing = enclosing.enclosingClass

        if (enclosing != null) builder.insert(0, "$")
    }

    return builder.insert(0, "$packageName.").toString()
}

fun <T> Class<T>.simpleString(): String {
    val builder: StringBuilder = StringBuilder(simpleName)

    var enclosing: Class<*>? = enclosingClass
    while (enclosing != null) {
        builder.insert(0, "${enclosing.simpleName}.")

        enclosing = enclosing.enclosingClass
    }

    return builder.toString()
}

fun <T> Class<T>.isAbstract(): Boolean {
    return Modifier.isAbstract(modifiers)
}

fun <T> Class<T>.visit(interfaces: Boolean = true): Sequence<Class<*>> = sequence {
    val queue = LinkedList<Class<*>>().apply { this += this@visit }

    while (queue.isNotEmpty()) {
        val next = queue.poll()

        if (next != Any::class.java) {
            yield(next)

            val `super` = next.superclass

            if (`super` != null && `super` != Any::class.java)
                queue.offer(`super`)

            if (interfaces) {
                for (i in next.interfaces) {
                    queue.offer(i)
                }
            }
        }
    }

    yield(Any::class.java)
}

val <T : Any> Class<T>.companion: Any?
    get() = kotlin.companionObjectInstance

val <T : Any> Class<T>.instance: T?
    get() = kotlin.instance

val <T : Any> Class<T>.isCompanion: Boolean
    get() = kotlin.isCompanion

val <T : Any> Class<T>.isObject: Boolean
    get() = kotlin.isCompanion && field("INSTANCE")?.isStatic == true

infix fun Class<*>.extends(other: Class<*>): Boolean = other.isAssignableFrom(this)

infix fun Class<*>.extends(other: KClass<*>): Boolean = other.java.isAssignableFrom(this)

private fun link(home: Class<*>, destination: Class<*>): LinkedList<Class<*>>? {
    if (home == destination)
        return LinkedList<Class<*>>()

    val iter = iterator<Class<*>> {
        if (home.superclass != null)
            yield(home.superclass)

        if (destination.isInterface)
            yieldAll(home.interfaces.iterator())
    }

    while (iter.hasNext()) {
        return link(iter.next(), destination)?.also { it.offerFirst(home) } ?: continue
    }

    return null
}

private fun linkBetween(c1: Class<*>, c2: Class<*>): LinkedList<Class<*>> {
    if (!c1.extends(c2))
        throw IllegalArgumentException("${c1.simpleString()} is not an instance of ${c2.simpleString()}!")

    return link(c1, c2) ?: throw NoSuchElementException()
}

private fun genericOf(c1: Class<*>, c2: Class<*>): ParameterizedType {
    if (!c2.isInterface)
        return c1.genericSuperclass as ParameterizedType

    val iter = c1.genericInterfaces.iterator()

    while (iter.hasNext()) {
        val next = iter.next()

        if (next !is ParameterizedType)
            continue

        if (next.rawType == c2)
            return next
    }

    throw NoSuchElementException()
}

private fun indexOf(cls: Class<*>, param: String): Int {
    val result = cls.typeParameters.indexOfFirst { p -> p.name == param }

    if (result == -1)
        throw NoSuchElementException()

    return result
}

fun Class<*>.typeInformationOf(cls: Class<*>): Map<String, Class<*>> {
    require(this extends cls) { "${simpleString()} is not an instance of ${cls.simpleString()}!" }

    if (cls.typeParameters.isEmpty())
        return emptyMap()

    val link = linkBetween(this, cls)
    var previous: Class<*> = cls
    val result: MutableMap<String, Class<*>> = mutableMapOf()
    val positions: Multimap<Int, String> = Multimaps.hashKeys().arrayListValues()

    for (i in cls.typeParameters.indices) {
        val param = cls.typeParameters[i]

        result[param.name] = Missing::class.java
        positions.put(i, param.name)
    }

    while (link.isNotEmpty() && !positions.isEmpty) {
        val current = link.pollLast()
        val genericInfo = genericOf(current, previous)

        for (i in genericInfo.actualTypeArguments.indices) {
            if (!positions.containsKey(i))
                continue

            when (val type = genericInfo.actualTypeArguments[i]) {
                is Class<*> ->
                    for (string in positions.removeAll(i))
                        result[string] = type

                is ParameterizedType ->
                    for (string in positions.removeAll(i))
                        result[string] = type.rawType as Class<*>

                is TypeVariable<*> -> {
                    val newIndex = indexOf(current, type.name)

                    for (string in positions.removeAll(i))
                        positions.put(newIndex, string)
                }
            }
        }

        previous = current
    }

    positions.values() iterate {
        result[it] = Any::class.java
    }

    return result
}

fun Class<*>.typeInformationOf(cls: KClass<*>): Map<String, Class<*>> = this.typeInformationOf(cls.java)

private class Missing
