package net.essentuan.esl.reflections.extensions

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

fun Type.typeArgs(): Array<Type> = if (this is ParameterizedType) this.actualTypeArguments else emptyArray()

fun Type.classOf(): Class<*> {
    return when (this) {
        is Class<*> -> this
        is ParameterizedType -> rawType.classOf();
        is WildcardType -> upperBounds.getOrElse(0) { Any::class.java }.classOf()
        is TypeVariable<*> -> bounds.getOrElse(0) { Any::class.java }.classOf()
        is GenericArrayType -> genericComponentType.classOf().arrayType()
        else -> error("Unexpected value: $this");
    };
}