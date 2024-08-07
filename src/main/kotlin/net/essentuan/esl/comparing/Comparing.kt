@file:OptIn(ExperimentalTypeInference::class)

package net.essentuan.esl.comparing

import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KProperty

object Comparing {
    fun <T : Comparable<T>> compare(c1: T?, c2: T?): Int {
        return compare(c1, c2, false)
    }

    fun <T : Comparable<T>> compare(c1: T?, c2: T?, nullGreater: Boolean): Int {
        return when {
            c1 === c2 -> 0
            c1 == null -> if (nullGreater) 1 else -1
            c2 == null -> if (nullGreater) -1 else 1
            else -> c1.compareTo(c2)
        }
    }
}

@JvmInline
value class Context<T>(val properties: MutableList<(T) -> Any?> = mutableListOf()) {
    operator fun KProperty<*>.unaryPlus() {
        prop { this.getter.call(it) }
    }

    fun prop(property: (T) -> Any?) {
        properties.add(property)
    }
}

@OverloadResolutionByLambdaReturnType
inline fun <reified T> T.equals(other: Any?, init: Context<T>.() -> Unit): Boolean {
    if (this === other)
        return true

    if (other !is T)
        return false

    val context = Context<T>()

    init(context)

    for (prop in context.properties)
        if (prop(this) != prop(other))
            return false

    return true
}

@JvmName("equalsBoolean")
inline fun <reified T> T.equals(other: Any?, init: Context<T>.(it: T, other: T) -> Boolean): Boolean {
    if (this === other)
        return true

    if (other !is T)
        return false

    val context = Context<T>()

    if (!init(context, this, other))
        return false

    for (prop in context.properties)
        if (prop(this) != prop(other))
            return false

    return true
}