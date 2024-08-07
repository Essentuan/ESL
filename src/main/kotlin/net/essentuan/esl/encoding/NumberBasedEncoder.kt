package net.essentuan.esl.encoding

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

abstract class NumberBasedEncoder<T: Number> : AbstractEncoder<T, Number> {
    constructor(cls: Class<T>) : super(cls, Number::class.java)

    constructor() : super()

    override fun encode(obj: T, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Number? = obj

    override fun toString(obj: T, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String = obj.toString()
}