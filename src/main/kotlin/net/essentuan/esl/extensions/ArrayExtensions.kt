package net.essentuan.esl.extensions

import net.essentuan.esl.iteration.extensions.stream
import java.util.Arrays
import java.util.stream.Stream

typealias JArray = java.lang.reflect.Array

@Suppress("UNCHECKED_CAST")
val <T> Array<T>.componentType: Class<T>
    get() = this.javaClass.componentType as Class<T>

@Suppress("UNCHECKED_CAST")
fun <T> array(type: Class<T>, size: Int): Array<T> = JArray.newInstance(type, size) as Array<T>

@Suppress("UNCHECKED_CAST")
inline fun <reified T> array(size: Int): Array<T> = arrayOfNulls<T>(size) as Array<T>

fun <T> Array<T>.resize(newSize: Int): Array<T> = Arrays.copyOf(this, newSize)

fun <T> Array<T>.extend(extra: Int): Array<T> = resize(size + extra)

fun <T> Array<T>.append(vararg elements: T): Array<T> {
    val new: Array<T> = extend(elements.size)
    System.arraycopy(elements, 0, new, size, elements.size)

    return new
}

fun <T> Array<T>.appendAll(vararg elements: Collection<T>): Array<T> {
    val total: Int = elements.sumOf { it.size }
    val new: Array<T> = this.extend(total)

    var i = size

    for (col in elements)
        for (e in col)
            new[i++] = e

    return new
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
fun <T> Array<T>.appendAll(vararg elements: Iterable<T>): Array<T> {
    val list = mutableListOf(*this)

    for (iterable in elements)
        list.addAll(iterable)

    return (list as java.util.List<T>).toArray(array(componentType, list.size))
}

fun <T> Array<T>.prepend(vararg elements: T): Array<T> {
    val new: Array<T> = Arrays.copyOf(elements, size + elements.size)
    System.arraycopy(this, 0, new, elements.size, size)

    return new
}

fun <T> Array<T>.prependAll(vararg elements: Collection<T>): Array<T> {
    val total: Int = elements.sumOf { it.size }
    val new: Array<T> = array(componentType, size + total)

    var i = 0

    for (col in elements)
        for (e in col)
            new[i++] = e

    System.arraycopy(this, 0, new, total, size)

    return new
}


fun <T> Array<T>.prependAll(vararg elements: Iterable<T>): Array<T> {
    val list = mutableListOf<T>()

    for (iterable in elements)
        list.addAll(iterable)

    list.addAll(this)

    return (list as java.util.List<T>).toArray(array(componentType, list.size))
}

fun <T> Array<T>.stream(): Stream<T> = iterator().stream()

fun Array<StackTraceElement>.build(): StringBuilder {
    val builder = java.lang.StringBuilder()
    for (i in indices) {
        builder
            .append("\t")
            .append(if (i == 0) "   " else "at ")
            .append(this[i].toString())
            .append("\n")
    }

    return builder
}