@file:OptIn(ExperimentalTypeInference::class)

package net.essentuan.esl

import net.essentuan.esl.other.repr
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.rx.RxState
import org.reactivestreams.Publisher
import org.reactivestreams.Subscription
import java.lang.ClassCastException
import java.util.NoSuchElementException
import java.util.Objects
import java.util.stream.Stream
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.java
import kotlin.jvm.javaClass

typealias KResult<T> = kotlin.Result<T>

interface Result<T> {
    class Value<T> internal constructor(val value: T) : Result<T> {
        override fun toString() = repr {
            +Value<T>::value
        }

        override fun hashCode() = Objects.hashCode(value)

        override fun equals(other: Any?) =
            if (other is Value<*>)
                other.value == value
            else
                other == value
    }

    class Fail<T> internal constructor(val cause: Throwable) : Result<T>

    object Empty : Result<Any?>

    companion object {
        fun <T> of(obj: T): Result<T> = Value(obj)

        fun <T> fail(cause: Throwable): Result<T> = Fail(cause)

        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): Result<T> = Empty as Result<T>
    }
}

inline fun <T> unsafe(block: () -> T): Result<T> {
    return try {
        return block().result()
    } catch (ex: Exception) {
        ex.fail()
    }
}

fun <T> T.result(): Result<T> = Result.of(this)

fun <T : Any> T?.ofNullable(): Result<T> = this?.result() ?: Result.empty()

fun <T> Throwable.fail(): Result<T> = Result.fail(this)

@OptIn(ExperimentalContracts::class)
fun <T> Result<T>.isEmpty(): Boolean {
    contract {
        returns(true) implies (this@isEmpty is Result.Empty)
    }

    return this is Result.Empty
}

@OptIn(ExperimentalContracts::class)
fun <T> Result<T>.isPresent(): Boolean {
    contract {
        returns(true) implies (this@isPresent is Result.Value)
    }

    return this is Result.Value
}

@OptIn(ExperimentalContracts::class)
fun <T> Result<T>.isFail(): Boolean {
    contract {
        returns(true) implies (this@isFail is Result.Fail)
    }

    return this is Result.Fail
}

fun <T> Result<T>.get(): T {
    return when (this) {
        is Result.Value -> value
        is Result.Fail -> throw cause
        else -> throw NoSuchElementException()
    }
}

inline fun <T> Result<T>.filter(predicate: (T) -> Boolean): Result<T> {
    if (!isPresent())
        return this

    return try {
        if (predicate(value))
            this
        else
            Result.empty()
    } catch (ex: Exception) {
        ex.fail()
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Result<*>.instanceOf(): Result<T> {
    if (this !is Result.Value<*>)
        return this as Result<T>

    return if (value is T)
        this as Result<T>
    else
        Result.empty()
}

@Suppress("UNCHECKED_CAST")
inline fun <reified T> Result<*>.cast(): Result<T> {
    if (this !is Result.Value<*>)
        return this as Result<T>

    return if (value is T)
        this as Result<T>
    else
        Result.fail(ClassCastException("${value?.javaClass?.simpleString()} cannot be cast to ${T::class.java.simpleString()}!"))
}

inline fun <T> Result<T>.filterNot(crossinline predicate: (T) -> Boolean): Result<T> =
    filter { !predicate(it) }

@Suppress("UNCHECKED_CAST")
fun <T : Any> Result<T?>.filterNotNull(): Result<T> {
    return when {
        !isPresent() -> this as Result<T>
        value == null -> Result.empty()
        else -> this as Result<T>
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <T, U> Result<T>.map(mapper: (T) -> U): Result<U> {
    if (!isPresent())
        return this as Result<U>

    return try {
        mapper(value).result()
    } catch (ex: Exception) {
        ex.fail()
    }
}

inline fun <T> Result<T>.peek(block: (T) -> Unit): Result<T> {
    if (isPresent())
        block(value)

    return this
}

@Suppress("UNCHECKED_CAST")
inline fun <T, U> Result<T>.flatMap(mapper: (T) -> Result<U>): Result<U> {
    if (!isPresent())
        return this as Result<U>

    return try {
        mapper(value)
    } catch (ex: Exception) {
        ex.fail()
    }
}

@OverloadResolutionByLambdaReturnType
inline infix fun <T> Result<T>.otherwise(mapper: () -> T): Result<T> {
    if (isPresent())
        return this

    return try {
        mapper().result()
    } catch (ex: Exception) {
        ex.fail()
    }
}

@JvmName("otherwiseFlatMap")
inline fun <T> Result<T>.otherwise(mapper: () -> Result<T>): Result<T> {
    if (isPresent())
        return this

    return try {
        mapper()
    } catch (ex: Exception) {
        ex.fail()
    }
}

@OverloadResolutionByLambdaReturnType
inline fun <T, reified EX> Result<T>.except(filter: (EX) -> Boolean = { true }, handler: (EX) -> T): Result<T> {
    if (!isFail())
        return this

    return try {
        if (cause is EX && filter(cause))
            handler(cause).result()
        else
            this
    } catch (ex: Exception) {
        ex.fail()
    }
}

@JvmName("exceptFlatMap")
inline fun <T, reified EX> Result<T>.except(filter: (EX) -> Boolean = { true }, handler: (EX) -> Result<T>): Result<T> {
    if (!isFail())
        return this

    return try {
        if (cause is EX && filter(cause))
            handler(cause)
        else
            this
    } catch (ex: Exception) {
        ex.fail()
    }
}


/**
 * When [this] is [Result.Fail] throws the contained exception.
 */
fun <T> Result<T>.raise(): Result<T> {
    if (isFail())
        throw cause

    return this
}

inline fun <T> Result<T>.ifPresent(block: (T) -> Unit) {
    if (isPresent())
        block(value)
}

inline fun <T> Result<T>.ifPresentOrElse(
    block: (T) -> Unit,
    empty: (Throwable?) -> Unit
) {
    if (isPresent())
        block(value)
    else
        empty(if (isFail()) cause else null)
}

fun <T> Result<T>.orElse(other: T): T {
    return if (isPresent()) value else other
}

inline fun <T> Result<T>.orElseGet(supplier: () -> T): T {
    return if (isPresent()) value else supplier()
}

fun <T : Any> Result<T>.orNull(): T? {
    return if (isPresent()) value else null
}

fun <T> Result<T>.orElseThrow(): T = get()

fun <T> Result<T>.orThrow(): T = get()

inline fun <T> Result<T>.orElseThrow(supplier: () -> Throwable): T {
    if (!isPresent())
        throw supplier()

    return value
}

inline infix fun <T> Result<T>.elif(block: () -> T): T =
    orElseGet(block)

inline fun <T> Result<T>.orThrow(supplier: () -> Throwable): T = orElseThrow(supplier)

fun <T> Result<T>.stream(): Stream<T> {
    return if (isEmpty()) Stream.empty() else Stream.of(orElseThrow())
}

fun <T> Result<T>.publish(): Publisher<T> = Publisher {
    it.onSubscribe(object : Subscription {
        var state = RxState.OPEN

        override fun request(n: Long) {
            require(n >= 0) { "n must be positive!" }

            if (state == RxState.OPEN && n > 0) {
                cancel()

                when (this@publish) {
                    is Result.Value<T> -> {
                        it.onNext(value)
                        it.onComplete()
                    }

                    is Result.Fail<T> -> it.onError(cause)
                    is Result.Empty -> it.onComplete()
                }
            }
        }

        override fun cancel() {
            state = RxState.CLOSED
        }
    })
}

fun <T> Result<T>.asSequence(): Sequence<T> {
    return if (isEmpty()) emptySequence() else sequenceOf(orElseThrow())
}

fun <T> KResult<T>.toResult(): Result<T> {
    return when {
        isFailure -> Result.fail<T>(exceptionOrNull()!!)
        else -> Result.of(getOrThrow())
    }
}
