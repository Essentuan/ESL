package net.essentuan.esl.reflections.extensions

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.jvm.kotlinFunction
import kotlin.reflect.jvm.kotlinProperty

val AnnotatedElement.tags: AnnotatedElement
    get() = this

infix fun <T : Annotation> AnnotatedElement.annotatedWith(annotation: Class<T>): Boolean = annotation in tags

infix fun <T : Annotation> AnnotatedElement.annotatedWith(annotation: KClass<T>): Boolean = annotation in tags

@Suppress("UNCHECKED_CAST")
operator fun <T : Annotation> AnnotatedElement.get(annotation: Class<T>): T? {
    if (isAnnotationPresent(annotation))
        return getAnnotation(annotation)

    return try {
        when (this) {
            is Field -> this.kotlinProperty?.annotations?.firstOrNull { it instanceof annotation } as T?
            is Class<*> -> this.kotlin.annotations.firstOrNull { it instanceof annotation } as T?
            is Method -> this.kotlinFunction?.annotations?.firstOrNull { it instanceof annotation } as T?
            else -> null
        }
    } catch (ex: Throwable) { null }
}

operator fun <T : Annotation> AnnotatedElement.get(annotation: KClass<T>): T? = this[annotation.java]

operator fun <T : Annotation> AnnotatedElement.contains(annotation: Class<T>): Boolean = this[annotation] != null

operator fun <T : Annotation> AnnotatedElement.contains(annotation: KClass<T>): Boolean = this[annotation] != null
