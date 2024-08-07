package net.essentuan.esl.reflections.extensions

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.reflect.jvm.kotlinProperty

val Field.isStatic: Boolean
    get() = Modifier.isStatic(modifiers)

val Field.isNullable: Boolean
    get() = kotlinProperty?.isNullable == true