package net.essentuan.esl.other

import net.essentuan.esl.reflections.extensions.simpleString
import java.util.function.Supplier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class Repr(val obj: Any): CharSequence {
    private val builder = StringBuilder()
    private var prefix = ""
    private var appender: Appender = Type.SQUARE_BRACKETS
    private var first: Boolean = true
    private var building: Boolean = true

    fun prefix(init: Prefix.() -> Unit) {
        this.prefix =
            if (first)
                Prefix().apply(init).prefix.toString()
            else
                unsupported("Cannot change prefix after writing has started")
    }

    fun prefix(cls: Class<*>) {
        prefix {
            + cls.simpleString()
        }
    }

    fun prefix(cls: KClass<*>) = prefix(cls.java)

    fun prefix(obj: Any) = prefix(obj.javaClass)

    fun with(appender: Supplier<Appender>) {
        this.appender = appender.get()
    }

    operator fun KProperty<*>.unaryPlus() {
        val value = this.getter.call(this@Repr.obj)

        this.name to when (value) {
            is BooleanArray? -> value.contentToString()
            is ByteArray? -> value.contentToString()
            is CharArray? -> value.contentToString()
            is DoubleArray? -> value.contentToString()
            is FloatArray? -> value.contentToString()
            is LongArray? -> value.contentToString()
            is IntArray? -> value.contentToString()
            is ShortArray? -> value.contentToString()
            is Array<*>? -> value.contentToString()
            else -> value.toString()
        }
    }

    infix fun String.to(value: Boolean?) = this to value.toString()

    infix fun String.to(array: BooleanArray?) = this to array.contentToString()

    infix fun String.to(value: Byte?) = this to value.toString()

    infix fun String.to(array: ByteArray?) = this to array.contentToString()

    infix fun String.to(value: Char?) = this to value.toString()

    infix fun String.to(array: CharArray?) = this to array.contentToString()

    infix fun String.to(value: Double?) = this to value.toString()

    infix fun String.to(array: DoubleArray?) = this to array.contentToString()

    infix fun String.to(value: Float?) = this to value.toString()

    infix fun String.to(array: FloatArray?) = this to array.contentToString()

    infix fun String.to(value: Int?) = this to value.toString()

    infix fun String.to(array: IntArray?) = this to array.contentToString()

    infix fun String.to(value: Long?) = this to value.toString()

    infix fun String.to(array: LongArray?) = this to array.contentToString()

    infix fun String.to(value: Short?) = this to value.toString()

    infix fun String.to(array: ShortArray?) = this to array.contentToString()

    infix fun String.to(array: Array<Any?>?) = this to array.contentToString()

    infix fun String.to(obj: Any?) {
        if (!building) unsupported("Cannot append to repr after toString has been called!")
        if (first) appender.open(builder.append(prefix))

        appender.append(builder, this, obj.toString(), first)
        first = false
    }

    enum class Type(private val opening: String, private val closing: String) : Appender {
        ROUND_BRACKETS("(", ")"),
        SQUARE_BRACKETS("[", "]"),
        CURLY_BRACKETS("{", "}"),
        ANGLE_BRACKETS("\u27E8", "\u27E9");

        override fun open(builder: StringBuilder) {
            builder.append(opening)
        }

        override fun close(builder: StringBuilder) {
            builder.append(closing)
        }

        override fun append(builder: StringBuilder, key: String, value: String?, first: Boolean) {
            if (!first) builder.append(", ")
            builder.append(key)
                .append('=')
                .append(value)
        }
    }

    interface Appender {
        fun open(builder: StringBuilder)
        fun close(builder: StringBuilder)

        fun append(builder: StringBuilder, key: String, value: String?, first: Boolean)
    }

    class Prefix internal constructor() {
        internal val prefix = StringBuilder()

        operator fun String.unaryPlus() {
            prefix.append(this)
        }
    }

    override val length: Int
        get() = builder.length

    override fun get(index: Int): Char = builder[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = builder.subSequence(startIndex, endIndex)

    override fun toString(): String {
        if (first) appender.open(builder.append(prefix))

        if (building) {
            appender.close(builder)
            building = false
        }

        return builder.toString()
    }
}

inline fun Any.repr(init: Repr.() -> Unit): String = Repr(this).apply(init).toString()

inline fun repr(init: Repr.() -> Unit): String = Repr(Unit).apply(init).toString()
