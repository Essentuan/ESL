package net.essentuan.esl.fetch

import net.essentuan.esl.fetch.handlers.Response
import net.essentuan.esl.json.Json

abstract class JsonRequest<T>(
    vararg args: Any?,
    rateLimit: RateLimit = NoLimit
) : Request<Json, T>(args = args, Response.json(), rateLimit)