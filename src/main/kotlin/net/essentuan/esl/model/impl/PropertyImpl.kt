package net.essentuan.esl.model.impl

import net.essentuan.esl.extensions.prepend
import net.essentuan.esl.model.Model
import net.essentuan.esl.model.annotations.Alias
import net.essentuan.esl.model.annotations.Override
import net.essentuan.esl.model.annotations.ReadOnly
import net.essentuan.esl.model.annotations.Root
import net.essentuan.esl.model.field.Field
import net.essentuan.esl.model.field.Property
import net.essentuan.esl.other.causedBy
import net.essentuan.esl.reflections.extensions.annotatedWith
import net.essentuan.esl.reflections.extensions.declaringClass
import net.essentuan.esl.reflections.extensions.get
import net.essentuan.esl.reflections.extensions.isDelegated
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.reflections.extensions.tags
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField

abstract class PropertyImpl(
    final override val element: KProperty<*>
) : Property {
    override val key: String? =
        if (this annotatedWith Root::class)
            null
        else
            tags[Override::class]?.value ?: element.name

    final override val delegate: Property.Delegate? =
        if (element.isDelegated)
            FieldDelegate(element.javaField!!)
        else
            null

    override val isMutable: Boolean =
        (element is KMutableProperty<*> || ReadWriteProperty::class.java.isAssignableFrom(element.javaField!!.type)) &&
                !(this annotatedWith ReadOnly::class)

    override val aliases: Array<String> =
        if (this annotatedWith Root::class)
            emptyArray()
        else
            tags[Alias::class]?.value?.prepend(key!!) ?: arrayOf(key!!)

    override val type: Field.Type = Field.Type(element.returnType, this)

    init {
        element.isAccessible = true
    }

    override fun get(model: Model<*>): Any? {
        if (delegate != null)
            delegate[model].getValue(model, element)

        return try {
            element.getter.call(model)
        } catch (ex: IllegalArgumentException) {
            if (ex.message?.startsWith("Callable expects") == false)
                throw ex

            element.getter.call()
        } catch (ex: Exception) {
            if (ex.causedBy<UninitializedPropertyAccessException>())
                return null

            throw ex
        }
    }

    override fun set(model: Model<*>, value: Any?) {
        when {
            delegate != null -> delegate[model].setValue(model, element, value)
            element !is KMutableProperty<*> ->
                throw IllegalStateException("Can't set immutable property ${element.name} in ${element.declaringClass.simpleString()}!")

            else -> {
                try {
                    element.setter.call(model, value)
                } catch (ex: IllegalArgumentException) {
                    if (ex.message?.startsWith("Callable expects") == false)
                        throw ex

                    element.setter.call(value)
                }
            }
        }
    }

    private class FieldDelegate(
        val field: java.lang.reflect.Field
    ) : Property.Delegate {
        override fun get(model: Model<*>): ReadWriteProperty<Any, Any?> {
            @Suppress("UNCHECKED_CAST")
            return field.get(model) as ReadWriteProperty<Any, Any?>
        }
    }
}