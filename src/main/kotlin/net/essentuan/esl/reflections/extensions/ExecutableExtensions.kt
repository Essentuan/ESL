package net.essentuan.esl.reflections.extensions

import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.Modifier

fun Executable.isStatic(): Boolean {
    return Modifier.isStatic(modifiers)
}