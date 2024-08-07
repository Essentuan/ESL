package net.essentuan.esl.model

import net.essentuan.esl.Rating
import net.essentuan.esl.encoding.JsonBasedEncoder
import net.essentuan.esl.json.Json
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.json.type.JsonType
import net.essentuan.esl.model.Model.Companion.export
import net.essentuan.esl.model.annotations.Ignored
import net.essentuan.esl.model.annotations.NoHash
import net.essentuan.esl.model.annotations.Sorted
import net.essentuan.esl.model.factories.DefaultFactory
import net.essentuan.esl.model.factories.OptionalFactory
import net.essentuan.esl.model.factories.PrimaryFactory
import net.essentuan.esl.model.field.Field
import net.essentuan.esl.model.field.Property
import net.essentuan.esl.other.repr
import net.essentuan.esl.other.unsupported
import net.essentuan.esl.reflections.extensions.annotatedWith
import net.essentuan.esl.reflections.extensions.extends
import net.essentuan.esl.reflections.extensions.get
import net.essentuan.esl.reflections.extensions.instanceof
import net.essentuan.esl.reflections.extensions.isDelegated
import net.essentuan.esl.reflections.extensions.isObject
import net.essentuan.esl.reflections.extensions.notinstanceof
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.reflections.extensions.typeInformationOf
import net.essentuan.esl.reflections.extensions.visit
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.kotlinProperty

class Descriptor<T : Model<JSON>, JSON : AnyJson> internal constructor(
    cls: Class<T>
) : Factory<T, JSON>, JsonBasedEncoder<T>(cls), Iterable<Property> {
    @Suppress("UNCHECKED_CAST")
    private val json: Class<JSON> = cls.typeInformationOf(Model::class)["OUT"] as Class<JSON>
    private val properties: List<Property>
    private val factory: Factory<T, JSON>

    init {
        val children = cls.visit().asIterable().reversed().asSequence().filter { it extends Model::class }.toList()
        val extensions = children.flatMap { Extension.of(it) }

        properties = children.asSequence()
            .flatMap { it.kotlin.declaredMemberProperties }
            .map {
                try {
                    if (
                        it annotatedWith Ignored::class ||
                        it.javaField == null ||
                        (it.isDelegated && !ReadWriteProperty::class.java.isAssignableFrom(it.javaField!!.type))
                    )
                        return@map null

                    synchronized(Field) {
                        val field = Field[it, cls.kotlin]
                        if (field != null)
                            return@map field as Property

                        for (extension in extensions) {
                            return@map extension(it)?.also { prop ->
                                Field[it, cls.kotlin] = prop
                            } ?: continue
                        }
                    }

                    return@map null
                } catch (ex: Exception) {
                    throw IllegalStateException(
                        "Failed to load property ${it.javaField!!.declaringClass.simpleString()}#${it.name}",
                        ex
                    )
                }
            }
            .filterNotNull()
            .run {
                val ordered = cls.visit(false)
                    .flatMap { it.declaredFields.asSequence() }
                    .filter { it.kotlinProperty != null }
                    .withIndex()
                    .associate { it.value.kotlinProperty!! to it.index }

                sortedBy { ordered[it.element] }
            }
            .sortedByDescending {
                it.element[Sorted::class]?.value ?: Rating.NORMAL
            }
            .toList()

        val kotlin = cls.kotlin
        val constructor = kotlin.primaryConstructor

        factory = when {
            kotlin.isObject -> Factory { _, _ -> unsupported("Cannot create new instance of object $kotlin!") }
            constructor == null || constructor.parameters.isEmpty() -> DefaultFactory(this, cls)
            constructor.parameters.any { it.isOptional } -> OptionalFactory(this, extensions, constructor)
            else -> PrimaryFactory(this, extensions, constructor)
        }
    }

    override operator fun invoke(data: JSON, flags: Set<Any>): T =
        factory(data, flags)

    fun load(model: T, `in`: JSON, flags: Set<Any>) {
        val data = model.process(`in`)

        for (prop in properties) {
            if (!prop.isMutable)
                continue

            prop.load(model, data, flags)

            if (!prop.type.isNullable && prop[model] == null)
                throw NullPointerException("${prop.element} is null!")
        }

        model.init(data)
    }

    fun export(model: T, flags: Set<Any>): JSON {
        model.prepare()

        val out = Json()

        for (prop in properties)
            prop.export(model, out, flags)

        @Suppress("UNCHECKED_CAST")
        val result = if (json == Json::class.java)
                    out as JSON
                else
                    JsonType.valueOf(out, json)

        return model.save(result)
    }

    fun hash(model: T): Int {
        var result = 1

        for (prop in properties) {
            if (prop annotatedWith NoHash::class)
                continue

            result = 31 * result + prop[model].hashCode()
        }

        return result
    }

    fun equals(model: T, other: Any?): Boolean {
        if (model === other)
            return true

        if (other notinstanceof model)
            return false

        for (prop in properties) {
            if (prop annotatedWith NoHash::class)
                continue

            if (prop[model] != prop[other as Model<*>])
                return false
        }

        return true
    }

    fun toString(model: T): String = repr {
        prefix(model.javaClass)

        properties.forEach { it.element.name to it[model] }
    }

    override fun encode(
        obj: T,
        flags: Set<Any>,
        type: Class<*>,
        element: AnnotatedElement,
        vararg typeArgs: Type
    ): AnyJson =
        obj.export(flags)

    @Suppress("UNCHECKED_CAST")
    override fun decode(
        obj: AnyJson,
        flags: Set<Any>,
        type: Class<*>,
        element: AnnotatedElement,
        vararg typeArgs: Type
    ): T = if (obj instanceof json)
        this(obj as JSON, flags)
    else
        this(JsonType.valueOf(obj, json), flags)

    override fun iterator(): Iterator<Property> = properties.iterator()
}