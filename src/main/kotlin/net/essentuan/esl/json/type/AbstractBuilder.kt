package net.essentuan.esl.json.type

import java.util.function.Supplier

abstract class AbstractBuilder<T : AnyJson, V : JsonType.Value, E : JsonType.Entry>(internal val jsonType: T) {
    abstract fun empty(): T

    abstract fun builder(): AbstractBuilder<T, V, E>

    infix operator fun String.invoke(init: AbstractBuilder<T, V, E>.() -> Unit) {
        this to builder().apply(init).jsonType
    }

    infix fun String.to(obj: Any?) {
        jsonType[this] = obj
    }

    @Suppress("UNCHECKED_CAST")
    operator fun String.unaryMinus(): V? = jsonType.remove(this) as V?

    fun cut(init: Moving.() -> Unit) {
        val cut = Moving().apply(init)

        jsonType.cut(cut.from, *cut.to.toTypedArray())
    }

    fun copy(init: Moving.() -> Unit) {
        val copy = Moving().apply(init)

        jsonType.copy(copy.from, *copy.to.toTypedArray())
    }

    class Moving {
        lateinit var from: String
        val to: MutableList<String> = ArrayList()

        inline fun from(from: () -> String) {
            this.from = from()
        }

        fun to(init: To.() -> Unit) {
            To(to).apply(init)
        }

        @JvmInline
        value class To(
            private val out: MutableList<String> = mutableListOf()
        ) {
            operator fun String.unaryPlus() = out.add(this)
        }
    }

    companion object {
        fun <T : AnyJson> create(empty: Supplier<T>, default: T? = null): AbstractBuilder<T, JsonType.Value, JsonType.Entry> {
            return object : AbstractBuilder<T, JsonType.Value, JsonType.Entry>(default ?: empty.get()) {
                override fun empty(): T = empty.get()

                override fun builder(): AbstractBuilder<T, JsonType.Value, JsonType.Entry> = create(empty)
            }
        }
    }
}