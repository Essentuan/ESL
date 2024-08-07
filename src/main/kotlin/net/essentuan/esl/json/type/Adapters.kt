package net.essentuan.esl.json.type

import net.essentuan.esl.collections.maps.Registry
import net.essentuan.esl.collections.maps.registry
import net.essentuan.esl.other.unsupported
import net.essentuan.esl.reflections.Reflections
import net.essentuan.esl.reflections.Types.Companion.objects
import net.essentuan.esl.reflections.extensions.instance
import net.essentuan.esl.reflections.extensions.instanceof
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.reflections.extensions.visit
import kotlin.collections.set

internal object Adapters {
    private val adapters = registry<Class<*>, Registry<Class<*>, JsonType.Adapter<*, *>>>(tracer = Class<*>::visit)

    init {
        Reflections.types
            .subtypesOf(JsonType.Adapter::class)
            .objects()
            .forEach {
                register(it.instance ?: return@forEach)
            }
    }

    private fun adapterFor(from: Class<*>, to: Class<*>): JsonType.Adapter<*, *>? = adapters[to]?.get(from)

    internal fun register(adapter: JsonType.Adapter<*, *>) {
        adapters.getOrPut(adapter.to()) { registry(tracer = Class<*>::visit) }[adapter.from()] = adapter
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <T: AnyJson> valueOf(obj: Any, cls: Class<T>): T {
        if (obj instanceof cls)
            return obj as T

        val adapter = adapterFor(obj.javaClass, cls) ?: unsupported("Cannot convert ${obj::class.simpleString()} to ${cls.simpleString()}!")

        return (adapter as JsonType.Adapter<Any, T>).convert(obj)
    }
}