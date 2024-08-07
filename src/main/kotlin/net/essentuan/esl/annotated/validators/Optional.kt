package net.essentuan.esl.annotated.validators

import net.essentuan.esl.annotated.AnnotatedType
import net.essentuan.esl.annotated.exceptions.ArgumentParameterException

class Optional<T : Annotation>(
    override val annotationClass: Class<T>,
    private val defaultValue: T
) : AnnotatedType.Validator<T> {
    @Throws(NoSuchElementException::class)
    override fun validate(cls: Class<*>) {
        //Empty
    }

    override fun valueOf(cls: Class<*>): T {
        return if (cls.isAnnotationPresent(annotationClass)) cls.getAnnotation(annotationClass) else defaultValue
    }
}
