package net.essentuan.esl.model

import net.essentuan.esl.collections.multimap.Multimaps
import net.essentuan.esl.collections.multimap.arrayListValues
import net.essentuan.esl.model.field.Parameter
import net.essentuan.esl.model.field.Property
import net.essentuan.esl.reflections.Reflections
import net.essentuan.esl.reflections.Types.Companion.objects
import net.essentuan.esl.reflections.extensions.instance
import net.essentuan.esl.reflections.extensions.typeInformationOf
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty

interface Extension<T: Model<*>> {
    operator fun invoke(param: KParameter): Parameter?

    operator fun invoke(property: KProperty<*>): Property?

    companion object {
        private val extensions = Multimaps.hashKeys().arrayListValues<Class<*>, Extension<*>>()

        init {
            Reflections.types
                .subtypesOf(Extension::class)
                .objects()
                .forEach {
                    val instance = it.instance!!

                    register(instance.typeInformationOf(Extension::class)["T"]!!, instance)
                }
        }

        fun register(cls: Class<*>, extension: Extension<*>) = extensions.put(cls, extension)

        fun of(cls: Class<*>): Iterable<Extension<*>> =
            extensions[cls]
    }
}