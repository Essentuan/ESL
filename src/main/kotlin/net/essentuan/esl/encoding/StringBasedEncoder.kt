package net.essentuan.esl.encoding

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

abstract class StringBasedEncoder<T: Any> : AbstractEncoder<T, String> {
    constructor(cls: Class<T>) : super(cls, String::class.java)

    constructor() : super()

    override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): T =
        decode(string, flags, type, element, *typeArgs)!!

    override fun toString(obj: T, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        encode(obj, flags, type, element, *typeArgs)!!
}