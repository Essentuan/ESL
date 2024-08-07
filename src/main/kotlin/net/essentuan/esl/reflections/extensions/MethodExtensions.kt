package net.essentuan.esl.reflections.extensions

import java.lang.reflect.Method

fun Method.descriptorString(): String {
    val descriptor = StringBuilder("(")

    for (c in parameterTypes) {
        descriptor.append(c.descriptorString())
    }
    descriptor.append(')')

    return descriptor.toString() + returnType.descriptorString()
}
