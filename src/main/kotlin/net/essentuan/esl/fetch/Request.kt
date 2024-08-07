package net.essentuan.esl.fetch

import net.essentuan.esl.Rating
import net.essentuan.esl.fetch.annotations.At
import net.essentuan.esl.fetch.annotations.Cache
import net.essentuan.esl.fetch.annotations.Timeout
import net.essentuan.esl.fetch.annotations.duration
import net.essentuan.esl.future.AbstractFuture
import net.essentuan.esl.reflections.extensions.get
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.reflections.extensions.tags
import net.essentuan.esl.time.duration.Duration
import net.essentuan.esl.time.duration.seconds
import java.net.HttpURLConnection.HTTP_OK
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.nio.charset.Charset
import java.util.Date

abstract class Request<BODY, OUT> : AbstractFuture<OUT?> {
    val uri: URI
    val body: BodyHandler<BODY>
    var cache: Duration?
        protected set
    val timeout: Duration
    val rateLimit: RateLimit
    var priority: Rating = Rating.NORMAL

    val userAgent: String?
        get() = null

    internal lateinit var start: Date

    constructor(
        uri: URI,
        body: BodyHandler<BODY>,
        cache: Duration?,
        timeout: Duration,
        rateLimit: RateLimit
    ) : super() {
        this.uri = uri
        this.body = body
        this.cache = cache
        this.timeout = timeout
        this.rateLimit = rateLimit
    }

    constructor(
        vararg args: Any?,
        body: BodyHandler<BODY>,
        rateLimit: RateLimit = NoLimit
    ) : super() {
        uri = URI.create(requireNotNull(javaClass.tags[At::class]) {
            "${javaClass.simpleString()} is missing @At!"
        }.value.format(*args))

        this.body = body

        cache = javaClass.tags[Cache::class]?.duration()
        timeout = javaClass.tags[Timeout::class]?.duration() ?: 60.seconds
        this.rateLimit = rateLimit
    }

    fun make(): HttpRequest.Builder {
        return HttpRequest.newBuilder(uri)
            .timeout(timeout.toJava())
            .apply {
                if (userAgent != null)
                    header("User-Agent", userAgent);
            }
    }

    @Throws(Throwable::class)
    protected abstract operator fun invoke(body: BODY): OUT?

    @Throws(Throwable::class)
    protected open fun validate(response: HttpResponse<BODY>): Boolean = response.statusCode() == HTTP_OK

    @Throws(Throwable::class)
    protected open fun after(response: HttpResponse<BODY>) = Unit

    @Throws(Throwable::class)
    protected open fun handle(response: HttpResponse<BODY>): OUT? {
        return if (validate(response)) this(response.body()) else null
    }

    open fun ready(): Boolean = rateLimit.ready(this)

    open fun fulfill(response: HttpResponse<BODY>): Boolean {
        try {
            val result = handle(response)

            after(response)

            complete(result)

            return result != null
        } catch(ex: Throwable) {
            raise(IllegalStateException("Failed to fetch $uri!", ex))

            return false
        }
    }

    internal fun error(ex: Throwable) = raise(ex)

    suspend fun execute(priority: Rating = Rating.NORMAL): OUT? {
        return (HttpScheduler.find(this) ?: this.apply {
            this.priority = priority
            HttpScheduler.enqueue(this)
        }).await()
    }
}

fun String.urlSafe(charset: Charset = Charsets.UTF_8): String = URLEncoder.encode(this, charset).replace("+", "%20")