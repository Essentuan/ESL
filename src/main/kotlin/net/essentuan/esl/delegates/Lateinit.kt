package net.essentuan.esl.delegates

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private typealias Provider<T> = PropertyDelegateProvider<Any?, ReadWriteProperty<Any?, T>>

open class Final<T> : Lateinit<T>()

open class Lateinit<T>: Lazy<T>, ReadWriteProperty<Any?, T> {
    private var ready: Boolean = false
    private var backing: T? = null

    open fun default(): T {
        throw UninitializedPropertyAccessException()
    }

    @Suppress("UNCHECKED_CAST")
    override var value: T
        get() {
            if (!ready) {
                backing = default()
                ready = true
            }

            return backing as T
        }
        set(value) {
            check(!ready || this !is Final<*>) { "Cannot set property after initialization!" }

            backing = value
            ready = true
        }

    override fun isInitialized(): Boolean = ready

    override fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

fun <T> lateinit(): Provider<T> = Provider { _, _ -> Lateinit() }

inline fun <T> lateinit(crossinline def: () -> T): Provider<T> = Provider { _, _ ->
    object : Lateinit<T>() {
        override fun default(): T = def()
    }
}

fun <T> final(): Provider<T> = Provider { _, _ -> Final() }

inline fun <T> final(crossinline def: () -> T): Provider<T> = Provider { _, _ ->
    object : Final<T>() {
        override fun default(): T = def()
    }
}
