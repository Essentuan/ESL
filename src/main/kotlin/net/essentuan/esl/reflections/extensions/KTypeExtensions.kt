package net.essentuan.esl.reflections.extensions

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

val KType.javaClass: Class<*>
    get() {
        if (classifier is KClass<*>)
            return jvm.java

        return javaType.classOf()
    }

val KType.jvm: KClass<*>
    get() {
        if (classifier is KClass<*>)
            return classifier as KClass<*>

        return javaClass.kotlin
    }