package net.essentuan.esl.encoding.builtin

import net.essentuan.esl.encoding.NumberBasedEncoder
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type

private typealias L = Long
private typealias I = Int
private typealias D = Double
private typealias F = Float

class Numbers {
    object Long : NumberBasedEncoder<L>() {
        override fun decode(obj: Number, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): L = obj.toLong()

        override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): L = string.toLong()
    }

    object Int : NumberBasedEncoder<I>() {
        override fun decode(obj: Number, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): I = obj.toInt()

        override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): I = string.toInt()
    }

    object Double : NumberBasedEncoder<D>() {
        override fun decode(obj: Number, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): D = obj.toDouble()

        override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): D = string.toDouble()
    }

    object Float : NumberBasedEncoder<F>() {
        override fun decode(obj: Number, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): F = obj.toFloat()

        override fun valueOf(string: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): F = string.toFloat()
    }
}