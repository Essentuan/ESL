package net.essentuan.esl.fetch.annotations

import net.essentuan.esl.fetch.RateLimit
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(val value: KClass<RateLimit>)
