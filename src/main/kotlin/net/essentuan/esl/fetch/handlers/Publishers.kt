package net.essentuan.esl.fetch.handlers

import net.essentuan.esl.json.Json
import net.essentuan.esl.model.Model
import net.essentuan.esl.model.Model.Companion.export
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.nio.charset.Charset

fun HttpRequest.Builder.POST(
    string: String,
    charset: Charset = Charsets.UTF_8
) = POST(BodyPublishers.ofString(string, charset))

fun HttpRequest.Builder.POST(
    json: Json,
    prettyPrint: Boolean = false,
    charset: Charset = Charsets.UTF_8
) = POST(json.asString(prettyPrint), charset)

fun HttpRequest.Builder.POST(
    model: Model<Json>,
    prettyPrint: Boolean = false,
    charset: Charset = Charsets.UTF_8
) = POST(model.export(), prettyPrint, charset)