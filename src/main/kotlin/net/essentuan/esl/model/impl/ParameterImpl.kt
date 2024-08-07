package net.essentuan.esl.model.impl

import net.essentuan.esl.extensions.prepend
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.model.annotations.Alias
import net.essentuan.esl.model.annotations.Override
import net.essentuan.esl.model.annotations.Root
import net.essentuan.esl.model.field.Field
import net.essentuan.esl.model.field.Parameter
import net.essentuan.esl.reflections.extensions.annotatedWith
import net.essentuan.esl.reflections.extensions.get
import net.essentuan.esl.reflections.extensions.tags
import kotlin.reflect.KParameter

private val MISSING_KEY = Any()

abstract class ParameterImpl(
    override val element: KParameter
) : Parameter {
    override val key: String? =
        if (this annotatedWith Root::class)
            null
        else
            tags[Override::class]?.value ?: element.name

    override val aliases: Array<String> =
        if (this annotatedWith Root::class)
            emptyArray()
        else
            tags[Alias::class]?.value?.prepend(key!!) ?: arrayOf(key!!)

    override val type: Field.Type = Field.Type(element.type, this)
    private val default: Any? = Parameter.default(type.cls)

    override fun Array<Any?>.append(data: AnyJson, index: Int, flags: Set<Any>): Int {
        val value = if (key == null)
            data
        else
            data.find().run {
                if (this == null)
                    MISSING_KEY
                else
                    raw
            }

        return when {
            value === MISSING_KEY || (value == null && !type.isNullable) -> {
                if (element.isOptional) {
                    this[index] = default

                    (1 shl (index % Integer.SIZE))
                } else
                    0
            }

            else -> {
                if (value != null)
                    this[index] =
                        type.encoder.decode(
                            value,
                            flags,
                            type.cls,
                            this@ParameterImpl,
                            *type.args
                        )

                0
            }
        }
    }
}