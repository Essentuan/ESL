package net.essentuan.esl.annotated.validators

import net.essentuan.esl.annotated.AnnotatedType
import net.essentuan.esl.annotated.exceptions.ArgumentParameterException

class Required<T : Annotation>(override val annotationClass: Class<T>) : AnnotatedType.Validator<T> {
    @Throws(NoSuchElementException::class)
    override fun validate(cls: Class<*>) {
        if (!cls.isAnnotationPresent(annotationClass))
            throw NoSuchElementException("Annotated class ${cls.simpleName} is missing required annotation ${annotationClass.simpleName}")
    }

    override fun valueOf(cls: Class<*>): T {
        return cls.getAnnotation(annotationClass)
    }
}
