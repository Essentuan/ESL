package net.essentuan.esl.model.factories

import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.model.Descriptor
import net.essentuan.esl.model.Extension
import net.essentuan.esl.model.Factory
import net.essentuan.esl.model.Model
import net.essentuan.esl.model.field.Field
import net.essentuan.esl.model.field.Parameter
import net.essentuan.esl.reflections.extensions.annotatedWith
import net.essentuan.esl.reflections.extensions.named
import java.lang.reflect.Constructor
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaConstructor

class OptionalFactory<T : Model<JSON>, JSON : AnyJson>(
    val descriptor: Descriptor<T, JSON>,
    extensions: List<Extension<*>>,
    primary: KFunction<T>
) : Factory<T, JSON> {
    val constructor: Constructor<T> =
        primary.javaConstructor?.named ?: throw IllegalArgumentException("Couldn't find name constructor for $primary")
    val parameters = primary.parameters
        .map {
            if (it annotatedWith Transient::class)
                return@map null

            synchronized(Field) {
                val field = Field[it, constructor.declaringClass.kotlin]
                if (field != null)
                    return@map field as Parameter

                for (extension in extensions)
                    return@map extension(it)?.also { prop ->
                        Field[it, constructor.declaringClass.kotlin] = prop
                    } ?: continue
            }

            null
        }
        .toList()

    init {
        constructor.isAccessible = true
    }

    override fun invoke(json: JSON, flags: Set<Any>): T {
        val arguments = arrayOfNulls<Any>(constructor.parameters.size)

        var mask = 0
        val masks = ArrayList<Int>(1)

        for (i in parameters.indices) {
            if (i != 0 && i % Int.SIZE_BITS == 0) {
                masks.add(mask)
                mask = 0
            }

            val param = parameters[i]

            if (param != null)
                mask = mask or param.run { arguments.append(json, i, flags) }
            else
                arguments[i] = Parameter.default(constructor.parameters[i].type)
        }

        masks.add(mask)

        for (i in masks.indices)
            arguments[parameters.size + i] = masks[i]

        val instance = constructor.newInstance(*arguments)
        descriptor.load(instance, json, flags)

        return instance
    }
}

