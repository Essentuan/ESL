package net.essentuan.esl.model.field

import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.model.Model
import net.essentuan.esl.reflections.extensions.instanceof
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface Property : Field {
    override val element: KProperty<*>
    val delegate: Delegate?

    val isMutable: Boolean

    operator fun get(model: Model<*>): Any?

    operator fun set(model: Model<*>, value: Any?)

    fun load(model: Model<*>, data: AnyJson, flags: Set<Any>) {
        val value = if (key == null)
            data
        else
            (data.find() ?: return).raw

        if (value == null) {
            if (type.isNullable)
                set(model, null)
        } else
            set(
                model,
                type.encoder.decode(
                    value,
                    flags,
                    type.cls,
                    this,
                    *type.args
                )
            )
    }

    fun export(model: Model<*>, out: AnyJson, flags: Set<Any>) {
        val result = get(model)?.run {
            type.encoder.encode(
                this,
                flags,
                type.cls,
                this@Property,
                *type.args
            )
        }

        when {
            key == null -> {
                if (result == null)
                    return

                if (result !is AnyJson)
                    throw IllegalStateException("Root fields must be json!")

                out.addAll(result)
            }
            (result instanceof out) -> {
                val obj = out[key!!]?.raw

                if (obj instanceof out)
                    (obj as AnyJson).addAll(result as AnyJson)
                else
                    out[key!!] = result
            }
            else -> out[key!!] = result
        }
    }

    fun interface Delegate {
        operator fun get(model: Model<*>): ReadWriteProperty<Any, Any?>
    }
}