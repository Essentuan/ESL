package net.essentuan.esl.annotated.validators

import net.essentuan.esl.annotated.AnnotatedType
import net.essentuan.esl.annotated.exceptions.ArgumentParameterException

class Placeholder<T : Annotation>(override val annotationClass: Class<T>) : AnnotatedType.Validator<T> {
    @Throws(NoSuchElementException::class)
    override fun validate(cls: Class<*>) {
        //As this class is only used to check if an annotation is present
        //there is no need to validate
    }

    override fun valueOf(cls: Class<*>): T {
        throw NoSuchElementException("Cannot net.essentuan.esl.get value of placeholder ")
    }
}
