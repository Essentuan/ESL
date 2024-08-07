package net.essentuan.esl.reflections.extensions

import net.essentuan.esl.elif
import net.essentuan.esl.orNull
import net.essentuan.esl.unsafe
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

private lateinit var OWNER_FIELD: Field

val KProperty<*>.declaringClass: KClass<*>
    get() {

        if (!::OWNER_FIELD.isInitialized)
            this.javaClass.visit(false)
                .map { unsafe { it.getDeclaredField("owner") }.orNull() }
                .filterNotNull()
                .firstOrNull()
                ?.also {
                    OWNER_FIELD = it
                    OWNER_FIELD.isAccessible = true
                }

        return unsafe {
            (OWNER_FIELD.get(this) as Class<*>).kotlin
        } elif {
            javaField?.declaringClass?.kotlin ?: javaGetter?.declaringClass?.kotlin!!
        }
    }

val KProperty<*>.isDelegated: Boolean
    get() = (javaField?.name == "$name\$delegate")

val KProperty0<*>.delegate: Any?
    get() {
        isAccessible = true

        return getDelegate()
    }

val KProperty0<*>.isReady: Boolean
    get() {
        val delegate = delegate ?: return true

        return delegate !is Lazy<*> || delegate.isInitialized()
    }

fun <T> KProperty0<T>.ifReady(block: (T) -> Unit) { if (isReady) block(get()) }

val <T: Any> KProperty0<T>.orNull: T?
    get() = if (isReady) get() else null