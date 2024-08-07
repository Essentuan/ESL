package net.essentuan.esl.reflections.extensions

import net.essentuan.esl.reflections.annotations.Null
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod

val KCallable<*>.isNullable: Boolean
    get() {
        if (returnType.isMarkedNullable || this annotatedWith Null::class)
            return true

        if (this is KProperty<*> && javaField?.annotatedWith(Null::class) == true)
            return true

        if (this is KFunction<*> && javaMethod?.annotatedWith(Null::class) == true)
            return true

        return false
    }