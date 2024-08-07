package net.essentuan.esl.annotated

import net.essentuan.esl.annotated.validators.Optional
import net.essentuan.esl.annotated.validators.Placeholder
import net.essentuan.esl.annotated.validators.Required
import java.util.function.Function
import kotlin.reflect.KClass

/**
 * A utility class to handle annotations
 */
open class AnnotatedType {
    interface Validator<T : Annotation> {
        val annotationClass: Class<T>

        @Throws(NoSuchElementException::class)
        fun validate(cls: Class<*>)

        fun valueOf(cls: Class<*>): T
    }

    private val cls: Class<*>

    private val validators: MutableMap<Class<out Annotation>, Validator<*>> = HashMap()

    constructor(vararg annotations: Validator<*>) {
        this.cls = javaClass

        register(*annotations)
    }

    constructor(cls: Class<*>, vararg annotations: Validator<*>) {
        this.cls = cls

        register(*annotations)
    }

    private fun register(vararg annotations: Validator<*>) {
        for (param in annotations) {
            param.validate(cls)

            validators[param.annotationClass] = param
        }
    }

    fun add(vararg annotations: Validator<*>) {
        register(*annotations)
    }

    fun <R : Annotation> has(annotation: Class<R>): Boolean {
        return cls.isAnnotationPresent(annotation)
    }

    fun <R : Annotation> has(annotation: KClass<R>): Boolean {
        return has(annotation.java)
    }


    fun <R : Annotation> get(annotation: Class<R>): R {
        return validators[annotation]!!.valueOf(cls) as R
    }

    fun <R : Annotation> get(annotation: KClass<R>): R {
        return get(annotation.java)
    }

    fun <A : Annotation, R> get(annotation: Class<A>, property: Function<A, R>): R {
        return property.apply(validators[annotation]!!.valueOf(cls) as A)
    }


    fun <A : Annotation, R> get(annotation: KClass<A>, property: Function<A, R>): R {
        return get(annotation.java, property)
    }

    companion object {
        fun <T : Annotation> required(cls: Class<T>): Validator<T> {
            return Required(cls)
        }

        fun <T : Annotation> required(cls: KClass<T>): Validator<T> {
            return Required(cls.java)
        }

        @Suppress("UNCHECKED_CAST")
        fun <T : Annotation> optional(defaultValue: T): Validator<T> {
            return Optional(defaultValue.annotationClass.java as Class<T>, defaultValue)
        }

        fun <T : Annotation> optional(cls: Class<T>, defaultValue: T): Validator<T> {
            return Optional(cls, defaultValue)
        }

        fun <T : Annotation> optional(cls: KClass<T>, defaultValue: T): Validator<T> {
            return Optional(cls.java, defaultValue)
        }

        fun <T : Annotation> placeholder(cls: Class<T>): Validator<T> {
            return Placeholder(cls)
        }

        fun <T : Annotation> placeholder(cls: KClass<T>): Validator<T> {
            return Placeholder(cls.java)
        }
    }
}
