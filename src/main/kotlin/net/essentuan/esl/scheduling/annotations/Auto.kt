package net.essentuan.esl.scheduling.annotations

import net.essentuan.esl.reflections.extensions.annotatedWith
import net.essentuan.esl.reflections.extensions.get
import net.essentuan.esl.reflections.extensions.tags
import net.essentuan.esl.reflections.extensions.visit
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Auto(
    val value: Boolean = true
)

private val autoLoad = mutableMapOf<Class<*>, Boolean>()

val KClass<*>.isAutoLoaded: Boolean
    get() = java.isAutoLoaded

val Class<*>.isAutoLoaded: Boolean
    get() = autoLoad.computeIfAbsent(this) compute@{
        this.visit().find { it annotatedWith Auto::class }?.tags?.get(Auto::class)?.value != false
    }

val Any.isAutoLoaded: Boolean
    get() = this.javaClass.isAutoLoaded
