package net.essentuan.esl.coroutines

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import net.essentuan.esl.Result
import net.essentuan.esl.future.api.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

abstract class Await<T> : CompletionHandler {
    protected abstract val nodes: Collection<Node>
    protected abstract val result: T
    private val count: AtomicInteger = AtomicInteger(0)
    private lateinit var cont: CancellableContinuation<T>

    open suspend fun await(): T {
        if (nodes.isEmpty())
            return result

        return suspendCancellableCoroutine { c ->
            cont = c

            count.set(nodes.size)
            nodes.forEach { it.attach() }

            if (c.isCompleted)
                dispose()
            else
                c.invokeOnCancellation(this)
        }
    }

    open operator fun invoke(node: Node, cause: Throwable?) = Unit

    final override fun invoke(cause: Throwable?) = dispose()

    private fun dispose() {
        nodes.forEach { it.dispose() }
    }

    open inner class Node(
        val job: Any
    ) : DisposableHandle, CompletionHandler {
        private var handle: DisposableHandle? = null

        open fun attach() {
            when(job) {
                is Future<*> -> job.handle {
                    invoke((it as? Result.Fail<*>)?.cause)
                }
                is Job -> {
                    job.start()
                    job.invokeOnCompletion(this)
                }
            }
        }

        @OptIn(InternalCoroutinesApi::class)
        override operator fun invoke(cause: Throwable?) {
            synchronized(this@Await) { this@Await(this, cause) }

            if (cause != null) {
                val token = cont.tryResumeWithException(cause)
                if (token != null) {
                    cont.completeResume(token)
                    this@Await.dispose()
                }
            } else if (count.decrementAndGet() == 0)
                cont.resume(result)
        }

        override fun dispose() {
            handle?.dispose()
            handle = null
        }
    }
}