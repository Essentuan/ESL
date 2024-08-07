package net.essentuan.esl.encoding

import net.essentuan.esl.reflections.Annotations
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

interface Provider<T: Any, OUT: Any> {
    val type: Class<T>

    operator fun invoke(
        cls: Class<in T>,
        element: AnnotatedElement = Annotations.empty(),
        vararg typeArgs: Type
    ): Encoder<T, OUT>
}