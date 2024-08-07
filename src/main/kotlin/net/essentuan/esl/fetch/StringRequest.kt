package net.essentuan.esl.fetch

import net.essentuan.esl.fetch.handlers.Response

abstract class StringRequest<T>(
    vararg args: Any?,
    rateLimit: RateLimit = NoLimit
) : Request<String, T>(args = args, Response.string(), rateLimit)