package net.essentuan.esl.scheduling.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Workers(val value: Int)
