package net.essentuan.esl.fetch

import java.net.http.HttpResponse

object NoLimit : RateLimit {
    override fun ready(request: Request<*, *>): Boolean = true

    override fun request(request: Request<*, *>) = Unit

    override fun response(response: HttpResponse<*>) = Unit
}