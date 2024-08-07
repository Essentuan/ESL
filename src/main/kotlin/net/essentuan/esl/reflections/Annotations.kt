package net.essentuan.esl.reflections

import java.lang.reflect.AnnotatedElement

object Annotations {
    fun empty(): AnnotatedElement = Empty
}

private object Empty : AnnotatedElement {
    override fun <T : Annotation?> getAnnotation(annotationClass: Class<T>): T? = null

    override fun getAnnotations(): Array<Annotation> = emptyArray()

    override fun getDeclaredAnnotations(): Array<Annotation> = emptyArray()
}