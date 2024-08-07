package net.essentuan.esl.model

import net.essentuan.esl.encoding.Encoder
import net.essentuan.esl.encoding.Provider
import net.essentuan.esl.json.type.AnyJson
import net.essentuan.esl.other.lock
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import kotlin.reflect.KClass

interface Model<OUT : AnyJson> {
    fun process(data: OUT): OUT = data

    fun init(data: OUT) = Unit

    fun prepare() = Unit

    fun save(data: OUT): OUT = data

    companion object : Provider<Model<*>, AnyJson> {
        private val descriptors = mutableMapOf<Class<*>, Descriptor<*, *>>()

        @Suppress("UNCHECKED_CAST")
        fun <T : Model<JSON>, JSON : AnyJson> descriptor(cls: Class<T>): Descriptor<T, JSON> {
            val value = descriptors[cls]

            if (value != null)
                return value as Descriptor<T, JSON>

            return descriptors.lock { getOrPut(cls) { Descriptor(cls) } as Descriptor<T, JSON> }
        }

        fun <T : Model<JSON>, JSON : AnyJson> descriptor(cls: KClass<T>): Descriptor<T, JSON> =
            descriptor(cls.java)

        inline operator fun <reified T : Model<JSON>, JSON : AnyJson> invoke(json: JSON, flags: Set<Any>) =
            descriptor(T::class.java)(json, flags)

        inline operator fun <reified T : Model<JSON>, JSON : AnyJson> invoke(json: JSON, vararg flags: Any): T =
            descriptor(T::class.java)(json, setOf(flags))

        fun <T : Model<JSON>, JSON : AnyJson> T.load(json: JSON, flags: Set<Any>) =
            descriptor(this.javaClass).load(this, json, flags)

        fun <T : Model<JSON>, JSON : AnyJson> T.load(json: JSON, vararg flags: Any) =
            load(json, setOf(*flags))

        fun <T : Model<JSON>, JSON : AnyJson> T.export(flags: Set<Any>): JSON =
            descriptor(this.javaClass).export(this, flags)

        fun <T : Model<JSON>, JSON : AnyJson> T.export(vararg flags: Any): JSON =
            export(setOf(*flags))

        @Suppress("UNCHECKED_CAST")
        override fun invoke(
            cls: Class<in Model<*>>,
            element: AnnotatedElement,
            vararg typeArgs: Type
        ): Encoder<Model<*>, AnyJson> =
            descriptor(cls as Class<Model<AnyJson>>) as Encoder<Model<*>, AnyJson>

        override val type: Class<Model<*>>
            get() = Model::class.java

        fun <T : Model<JSON>, JSON : AnyJson> JSON.wrap(descriptor: Descriptor<T, JSON>, flags: Set<Any>): T =
            descriptor(this, flags)

        fun <T : Model<JSON>, JSON : AnyJson> JSON.wrap(descriptor: Descriptor<T, JSON>, vararg flags: Any): T =
            descriptor(this, setOf(flags))

        fun <T : Model<JSON>, JSON : AnyJson> JSON.wrap(cls: Class<T>, flags: Set<Any>): T =
            wrap(descriptor(cls), flags)

        fun <T : Model<JSON>, JSON : AnyJson> JSON.wrap(cls: Class<T>, vararg flags: Any): T =
            wrap(descriptor(cls), flags)

        fun <T : Model<JSON>, JSON : AnyJson> JSON.wrap(cls: KClass<T>, flags: Set<Any>): T =
            wrap(descriptor(cls), flags)

        fun <T : Model<JSON>, JSON : AnyJson> JSON.wrap(cls: KClass<T>, vararg flags: Any): T =
            wrap(descriptor(cls), flags)

        inline fun <reified T : Model<JSON>, JSON : AnyJson> JSON.wrap(flags: Set<Any>): T =
            wrap(descriptor(T::class.java), flags)

        inline fun <reified T : Model<JSON>, JSON : AnyJson> JSON.wrap(vararg flags: Any): T =
            wrap(descriptor(T::class.java), flags)
    }
}