package net.essentuan.esl.fetch


import net.essentuan.esl.Rating
import net.essentuan.esl.collections.maps.expireAfter
import net.essentuan.esl.collections.synchronized
import net.essentuan.esl.delegates.final
import net.essentuan.esl.iteration.extensions.mutable.iterate
import net.essentuan.esl.other.lock
import net.essentuan.esl.time.TimeUnit
import net.essentuan.esl.time.duration.FormatFlag
import net.essentuan.esl.time.extensions.timeSince
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.util.Date
import java.util.PriorityQueue
import java.util.Queue
import java.util.concurrent.TimeoutException

object HttpScheduler {
    private val client: HttpClient by lazy { HttpClient.newHttpClient() }

    private val queue: Queue<Request<*, *>> = PriorityQueue(16) { e, e2 -> e.priority.compareTo(e2.priority) }

    private val cache = mutableMapOf<URI, Request<*, *>>()
        .synchronized()
        .expireAfter { (_, it) -> it.cache!! }

    fun cleanse() = cache.lock { cleanse() }

    private fun fill() {
        queue.lock {
            iterate {
                when {
                    it.uri in Outbound -> remove()
                    it.ready() && Outbound.ready(it) -> {
                        remove()

                        send(it)
                    }
                }
            }
        }
    }

    private fun <BODY> send(request: Request<BODY, *>) {
        request.rateLimit.request(request)

        Outbound += request

        request.start = Date()

        client.sendAsync(request.make().build(), request.body)
            .whenComplete { value, ex ->
                if (ex == null)
                    success(request, value)
                else
                    fail(request, ex)

                fill()
            }
    }

    private fun <BODY> success(request: Request<BODY, *>, response: HttpResponse<BODY>) {
        try {
            request.rateLimit.response(response)

            if (request.fulfill(response) && request.cache != null)
                cache.lock { put(request.uri, request) }
        } catch (ex: Throwable) {
            request.error(ex)
        }
    }

    private fun <BODY> fail(request: Request<BODY, *>, throwable: Throwable) {
        if (throwable is HttpTimeoutException)
            request.error(
                TimeoutException(
                    "Request for ${request.uri} has timed out after ${
                        request.start.timeSince().print(
                            FormatFlag.COMPACT, TimeUnit.MILLISECONDS
                        )
                    }"
                )
            )
        else
            request.error(throwable)
    }

    internal fun enqueue(request: Request<*, *>) {
        queue.lock { offer(request) }

        fill()
    }

    @Suppress("UNCHECKED_CAST")
    internal fun <OUT> find(request: Request<*, OUT>): Request<*, OUT>? {
        val outbound = Outbound[request]
        if (outbound != null)
            return outbound as Request<*, OUT>

        cache.lock {
            val cached = get(request.uri)
            if (cached != null)
                return cached as Request<*, OUT>
        }

        queue.lock {
            for (req in queue) {
                if (req.uri == request.uri)
                    return req as Request<*, OUT>
            }
        }

        return null
    }

    object Outbound {
        var size = 0
            private set

        private val backing = mutableMapOf<String, MutableMap<URI, Request<*, *>>>()

        val base = Value(30)
        val max = Value(50)

        fun ready(request: Request<*, *>): Boolean {
            val host = backing.lock { getOrDefault(request.uri.host, mapOf()) }

            return host.size < capacity(request.uri, request.priority)
        }

        operator fun plusAssign(request: Request<*, *>) {
            backing.lock {
                val host = computeIfAbsent(request.uri.host) {
                    mutableMapOf()
                }

                val size = host.size

                host[request.uri] = request

                this@Outbound.size += (host.size - size)
            }

            request.exec {
                remove(request)
                fill()
            }
        }


        operator fun get(uri: URI): Request<*, *>? = backing.lock {
            get(uri.host)?.get(uri)
        }

        operator fun get(request: Request<*, *>): Request<*, *>? = get(request.uri)

        operator fun contains(uri: URI): Boolean = this[uri] != null

        operator fun contains(request: Request<*, *>): Boolean = request.uri in this

        fun remove(request: Request<*, *>) {
            backing.lock {
                if (get(request.uri.host)?.remove(request.uri) != null) {
                    this@Outbound.size++
                }
            }
        }

        fun capacity(hostname: String, priority: Rating): Int {
            val divisor: Double = Rating.entries.size - 3.0

            val base = this.base[hostname]
            val max = this.max[hostname]

            return (base + ((max - base) / divisor) * (divisor - priority.ordinal)).toInt()
        }

        fun capacity(uri: URI, priority: Rating): Int = capacity(uri.host, priority)

        class Value internal constructor(default: Int) {
            private val backing = mutableMapOf<String, Int>()

            var default: Int by final { default }

            operator fun set(hostname: String, size: Int) {
                backing[hostname] = size
            }

            operator fun set(uri: URI, size: Int) {
                this[uri.host] = size
            }

            operator fun get(hostname: String): Int = backing.getOrDefault(hostname, default)

            operator fun get(uri: URI) = this[uri.host]
        }
    }
}