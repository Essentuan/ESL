package net.essentuan.esl.model.factories

import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.model.Descriptor
import net.essentuan.esl.model.Extension
import net.essentuan.esl.model.Factory
import net.essentuan.esl.model.Model
import net.essentuan.esl.model.annotations.Ignored
import net.essentuan.esl.model.field.Field
import net.essentuan.esl.model.field.Parameter
import net.essentuan.esl.reflections.extensions.annotatedWith
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaConstructor

class PrimaryFactory<T : Model<JSON>, JSON : AnyJson>(
    val descriptor: Descriptor<T, JSON>,
    extensions: List<Extension<*>>,
    callable: KFunction<T>
) : Factory<T, JSON> {
    val constructor = callable.javaConstructor!!
    val parameters = callable.parameters
        .map {
            if (it annotatedWith Ignored::class)
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

        for (i in parameters.indices) {
            val param = parameters[i]

            if (param != null)
                param.apply { arguments.append(json, i, flags) }
            else
                arguments[i] = Parameter.default(constructor.parameters[i].type)
        }

        val instance = constructor.newInstance(*arguments)
        descriptor.load(instance, json, flags)

        return instance
    }
}