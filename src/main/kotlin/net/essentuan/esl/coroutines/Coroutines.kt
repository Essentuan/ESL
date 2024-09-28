package net.essentuan.esl.coroutines

import kotlinx.coroutines.*
import net.essentuan.esl.Result
import net.essentuan.esl.future.api.Future
import net.essentuan.esl.ifPresent
import net.essentuan.esl.time.duration.Duration
import java.util.concurrent.ForkJoinPool
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KProperty

val COMMON_POOL = ForkJoinPool.commonPool().asCoroutineDispatcher()

inline fun <T> dispatch(
    context: CoroutineContext = Dispatchers.Default,
    crossinline block: suspend CoroutineScope.() -> T
): Future<T> =
    Future {
        withContext(context) {
            block()
        }
    }

@Deprecated("", ReplaceWith("dispatch { block() }"))
inline fun <T> async(crossinline block: suspend CoroutineScope.() -> T): Future<T> =
    dispatch { block() }

@Deprecated("", ReplaceWith("dispatch { block() }"))
inline fun launch(crossinline block: suspend CoroutineScope.() -> Unit): Future<Unit> =
    dispatch { block() }

@Deprecated("", ReplaceWith("dispatch(Dispatchers.IO) { block() }", "kotlinx.coroutines.Dispatchers"))
inline fun <T> fork(crossinline block: suspend CoroutineScope.() -> T): Future<T> =
    dispatch(Dispatchers.IO) { block() }

@Throws(InterruptedException::class)
fun <T> blocking(context: CoroutineContext = EmptyCoroutineContext, block: suspend CoroutineScope.() -> T): T =
    runBlocking(context, block)

suspend fun <T> timeout(timeout: Duration, block: suspend CoroutineScope.() -> T): T =
    withTimeout(timeout.toKotlin(), block)

suspend fun delay(delay: Duration) = kotlinx.coroutines.delay(delay.toKotlin())

suspend inline fun await(init: Collector.() -> Unit) {
    val collector = Collector()

    collector.init()

    return collector.await()
}

suspend inline fun <K, V> group(init: Group<K, V>.() -> Unit): Map<K, V> =
    Group<K, V>().apply(init).await()

operator fun <T> Future<T>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): Delegate<T> = Delegate(this::await)

operator fun <T> Deferred<T>.provideDelegate(
    thisRef: Any?,
    property: KProperty<*>
): Delegate<T> = Delegate(this::await)

class Collector : Await<Unit>() {
    override val nodes: MutableList<Node> = mutableListOf()
    override val result: Unit
        get() = Unit

    operator fun Future<*>.unaryPlus() {
        nodes += Node(this)
    }

    operator fun Job.unaryPlus() {
        nodes += Node(this)
    }
}

class Group<K, V> : Await<Map<K, V>>() {
    override val nodes: MutableList<Node> = mutableListOf()
    override val result: MutableMap<K, V> = mutableMapOf()

    @Synchronized
    infix fun K.to(value: V) {
        result[this] = value
    }

    infix fun K.by(deferred: Deferred<V>) {
        nodes += Key(this, deferred)
    }

    infix fun K.by(future: Future<V>) {
        nodes += Key(this, future)
    }

    inline operator fun K.invoke(crossinline block: suspend () -> V) = this by Future(block)

    @OptIn(ExperimentalCoroutinesApi::class)
    @Suppress("UNCHECKED_CAST")
    override fun invoke(node: Node, cause: Throwable?) {
        if (cause != null)
            return

        when (val job = (node as Key?)?.job ?: return) {
            is Future<*> -> {
                job.result().ifPresent { result[node.key] = it as V }
            }

            is Deferred<*> -> {
                result[node.key] = job.getCompleted() as V
            }
        }
    }

    inner class Key(val key: K, job: Any) : Node(job)
}

//class Grouping<K, V>(private val values: MutableMap<K, Any?> = mutableMapOf(), val scope: CoroutineScope) {
//    operator fun K.invoke(
//        context: CoroutineContext = EmptyCoroutineContext,
//        start: CoroutineStart = CoroutineStart.DEFAULT,
//        block: suspend CoroutineScope.() -> V
//    ) = this by scope.async(context, start, block)
//
//    infix fun K.to(value: V) = this by Future(value)
//
//    infix fun K.by(deferred: Deferred<V>) {
//        values[this] = deferred
//    }
//
//    infix fun K.by(future: Future<V>) {
//        values[this] = future
//    }
//
//    inline operator fun K.invoke(crossinline block: suspend () -> V) = this by Future(block)
//
//    @Suppress("UNCHECKED_CAST")
//    @OptIn(ExperimentalCoroutinesApi::class)
//    suspend fun await(): Map<K, V> {
//    }
//}

class Delegate<T>(val getter: suspend () -> T) {
    var result = Result.empty<T>()

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = when (val it = result) {
        is Result.Value -> it.value
        is Result.Fail -> error(it.cause)
        else -> blocking {
            val out = getter()
            result = Result.of(out)

            return@blocking out
        }
    }
}


