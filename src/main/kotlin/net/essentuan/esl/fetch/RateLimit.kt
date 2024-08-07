package net.essentuan.esl.fetch

import java.net.http.HttpResponse

interface RateLimit {
    fun ready(request: Request<*, *>): Boolean

    fun request(request: Request<*, *>)

    fun response(response: HttpResponse<*>)
}