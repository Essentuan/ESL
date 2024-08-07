package net.essentuan.esl.model.annotations


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
annotation class Alias(val value: Array<String>)
