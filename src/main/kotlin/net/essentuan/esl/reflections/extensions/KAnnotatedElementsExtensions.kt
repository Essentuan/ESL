package net.essentuan.esl.reflections.extensions

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

val KAnnotatedElement.tags: KAnnotatedElement
    get() = this

infix fun <U: Annotation> KAnnotatedElement.annotatedWith(other: Class<U>): Boolean = other in this

infix fun <U: Annotation> KAnnotatedElement.annotatedWith(other: KClass<U>): Boolean = other in this

@Suppress("UNCHECKED_CAST")
operator fun <T : Annotation> KAnnotatedElement.get(annotation: KClass<T>): T? =
    annotations.firstOrNull { it instanceof annotation } as T?

operator fun <T : Annotation> KAnnotatedElement.get(annotation: Class<T>): T? = get(annotation.kotlin)

operator fun <T : Annotation> KAnnotatedElement.contains(annotation: KClass<T>): Boolean = this[annotation] != null

operator fun <T : Annotation> KAnnotatedElement.contains(annotation: Class<T>): Boolean = this[annotation] != null


