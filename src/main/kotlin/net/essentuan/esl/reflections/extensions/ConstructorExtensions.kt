package net.essentuan.esl.reflections.extensions

import java.lang.reflect.Constructor

private val DEFAULT_CONSTRUCTOR_MARKER = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")

val <T> Constructor<T>.named: Constructor<T>?
    get() {
        val expectedSize = parameters.size + Math.ceil(parameters.size / Int.SIZE_BITS.toDouble()).toInt() + 1

        for (constructor in declaringClass.declaredConstructors) {
            if (constructor.parameters.size != expectedSize)
                continue

            for (i in constructor.parameters.indices) {
                val param = constructor.parameters[i]

                when {
                    i < parameters.size ->
                        if (param.type != parameters[i].type)
                            break

                    i < (constructor.parameters.size - 1) ->
                        if (param.type != Integer.TYPE)
                            break

                    else -> {
                        if (param.type == DEFAULT_CONSTRUCTOR_MARKER)
                            @Suppress("UNCHECKED_CAST")
                            return constructor as Constructor<T>
                    }
                }
            }
        }

        return null
    }