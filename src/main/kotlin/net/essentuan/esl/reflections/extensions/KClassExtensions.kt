package net.essentuan.esl.reflections.extensions

import net.essentuan.esl.except
import net.essentuan.esl.orElse
import net.essentuan.esl.other.lock
import net.essentuan.esl.raise
import net.essentuan.esl.unsafe
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KDeclarationContainer

fun <T : Any> KClass<T>.isAbstract(): Boolean {
    return java.isAbstract()
}

fun <T : Any> KClass<T>.constructor(vararg args: Class<*>): Constructor<T>? {
    return java.constructor(*args)
}

fun <T : Any> KClass<T>.hasConstructor(vararg args: Class<*>): Boolean {
    return java.hasConstructor(*args)
}

/**
 * Constructs a new object
 */
fun <T : Any> KClass<T>.construct(vararg args: Any): T? {
    return java.construct(*args)
}

fun <T : Any> KClass<T>.hasMethod(name: String, vararg args: Class<*>): Boolean {
    return java.hasMethod(name, *args)
}

fun <T : Any> KClass<T>.method(name: String, vararg args: Class<*>): Method? {
    return java.method(name, *args)
}

/**
 * Invokes a static method
 */
fun <T : Any> KClass<T>.invoke(name: String, vararg args: Any): Any? {
    return java.invoke(name, *args);
}

fun <T : Any> KClass<T>.field(name: String): Field? {
    return java.field(name);
}

fun <T : Any> KClass<T>.hasField(name: String): Boolean {
    return java.hasField(name);
}

/**
 * Sets a static field
 */
fun <T : Any> KClass<T>.set(field: String, value: Any) {
    return java.set(field, value);
}

/**
 * Gets a static field
 */
fun <T : Any, U> KClass<T>.get(field: String, value: Any): U? {
    return java.get(field, value);
}

fun <T : Any> KClass<T>.fullString(): String {
    val builder = StringBuilder()

    var enclosing: Class<*>? = this.java.enclosingClass
    while (enclosing != null) {
        builder.insert(0, enclosing.simpleName)
        enclosing = enclosing.enclosingClass

        if (enclosing != null) builder.insert(0, "$")
    }

    return builder.insert(0, "${java.packageName}.").toString()
}

fun <T : Any> KClass<T>.simpleString(): String {
    val builder: StringBuilder = StringBuilder(simpleName)

    var enclosing: Class<*>? = this.java.enclosingClass
    while (enclosing != null) {
        builder.insert(0, "${enclosing.simpleName}.")

        enclosing = enclosing.enclosingClass
    }

    return builder.toString()
}

infix fun KClass<*>.extends(other: Class<*>): Boolean = other.isAssignableFrom(this.java)

infix fun KClass<*>.extends(other: KClass<*>): Boolean = other.java.isAssignableFrom(this.java)

fun KClass<*>.visit(interfaces: Boolean = true): Sequence<Class<*>> = java.visit(interfaces)

private val instances = mutableMapOf<KClass<*>, Any?>()

@Suppress("UNCHECKED_CAST")
val <T : Any> KClass<T>.instance: T?
    get() =
        unsafe {
            objectInstance
        }.except { _: IllegalAccessException ->
            instances.lock {
                computeIfAbsent(this@instance) {
                    val field =
                        if (isCompanion)
                            java.enclosingClass.getDeclaredField(java.simpleName)
                        else
                            java.getDeclaredField("INSTANCE")

                    field.isAccessible = true
                    field.get(null)
                }
            } as T?
        }.raise().orElse(null)

fun KClass<*>.typeInformationOf(cls: Class<*>): Map<String, Class<*>> = java.typeInformationOf(cls)

fun KClass<*>.typeInformationOf(cls: KClass<*>): Map<String, Class<*>> = java.typeInformationOf(cls)

val KClass<*>.isObject: Boolean
    get() = isCompanion || field("INSTANCE")?.isStatic == true

private val method by lazy {
    Class.forName("kotlin.jvm.internal.Reflection")
        .getDeclaredMethod("getOrCreateKotlinPackage", Class::class.java)
}

private val getOrCreatePackage: (Class<*>) -> KDeclarationContainer by lazy {
    { method.invoke(null, it) as KDeclarationContainer }
}

val KClass<*>.`package`: KDeclarationContainer
    get() = getOrCreatePackage(java)
