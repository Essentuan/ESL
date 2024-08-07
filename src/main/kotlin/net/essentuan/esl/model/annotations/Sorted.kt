package net.essentuan.esl.model.annotations

import net.essentuan.esl.Rating

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Sorted(val value: Rating)
