package net.essentuan.esl.reflections.extensions

import net.essentuan.esl.filter
import net.essentuan.esl.filterNot
import net.essentuan.esl.map
import net.essentuan.esl.ofNullable
import net.essentuan.esl.orElse
import net.essentuan.esl.orNull
import net.essentuan.esl.raise
import net.essentuan.esl.unsafe
import java.lang.reflect.Field
import kotlin.reflect.KClass

/**
 * Invokes an instance method
 */
@Suppress("CheckedExceptionsKotlin")
fun Any.invoke(name: String, vararg args: Any): Any? {
    return unsafe {
        this::class.method(name, *args.map { e -> e.javaClass }.toTypedArray())?.invoke(null, args)
    }.raise().orElse(null)
}

/**
 * Sets an instance field
 */
fun Any.set(field: String, value: Any) {
    this::class.field(field)
        .ofNullable()
        .filterNot(Field::isStatic)
        .filter(Field::trySetAccessible)
        .map { it.set(this, value) }
        .raise()
}

/**
 * Gets an instance field
 */
@Suppress("UNCHECKED_CAST")
fun <T> Any.get(field: String, value: Any): T? {
    return this::class.field(field)
        .ofNullable()
        .filterNot(Field::isStatic)
        .filter(Field::trySetAccessible)
        .map { it.get(this) }
        .raise()
        .orNull() as T
}

infix fun Any?.instanceof(cls: Class<*>): Boolean {
    return cls.isInstance(this ?: return false)
}

infix fun Any?.instanceof(cls: KClass<*>): Boolean {
    return cls.isInstance(this ?: return false)
}

infix fun Any?.instanceof(obj: Any?): Boolean {
    if (this === obj)
        return true

    return this instanceof (obj?.javaClass ?: return false)
}

infix fun Any?.notinstanceof(cls: Class<*>): Boolean =
    !(this instanceof cls)

infix fun Any?.notinstanceof(cls: KClass<*>): Boolean =
    !(this instanceof cls)

infix fun Any?.notinstanceof(obj: Any?): Boolean =
    !(this instanceof obj)

fun Any.typeInformationOf(cls: Class<*>): Map<String, Class<*>> = javaClass.typeInformationOf(cls)

fun Any.typeInformationOf(cls: KClass<*>): Map<String, Class<*>> = javaClass.typeInformationOf(cls)