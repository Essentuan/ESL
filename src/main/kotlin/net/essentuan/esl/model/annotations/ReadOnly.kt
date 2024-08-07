package net.essentuan.esl.model.annotations

/**
 * Informs the descriptor this property should only be read.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY)
annotation class ReadOnly()
