package net.essentuan.esl.fetch.handlers

import net.essentuan.esl.json.Json
import net.essentuan.esl.json.type.GsonTypes
import java.io.InputStreamReader
import java.net.http.HttpResponse
import java.net.http.HttpResponse.BodyHandler
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.Charset
import kotlin.streams.asSequence

object Response {
    fun json(): BodyHandler<Json> = BodyHandler {
        HttpResponse.BodySubscribers.mapping(
            HttpResponse.BodySubscribers.ofInputStream()
        ) {
            Json(GsonTypes.DEFAULT.newJsonReader(InputStreamReader(it)))
        }
    }

    fun json(charset: Charset): BodyHandler<Json> = BodyHandler {
        HttpResponse.BodySubscribers.mapping(
            HttpResponse.BodySubscribers.ofInputStream()
        ) {
            Json(GsonTypes.DEFAULT.newJsonReader(InputStreamReader(it, charset)))
        }
    }

    fun string(): BodyHandler<String> = BodyHandlers.ofString()

    fun string(charset: Charset): BodyHandler<String> = BodyHandlers.ofString(charset)

    fun lines(): BodyHandler<Sequence<String>> = BodyHandler {
        HttpResponse.BodySubscribers.mapping(
            BodyHandlers.ofLines().apply(it)
        ) { s -> s.asSequence() }
    }

    fun lines(charset: Charset): BodyHandler<Sequence<String>> = BodyHandler {
        HttpResponse.BodySubscribers.mapping(
            HttpResponse.BodySubscribers.ofLines(charset)
        ) { s -> s.asSequence() }
    }

    @Suppress("UNCHECKED_CAST")
    fun discard(): BodyHandler<Unit> = HttpResponse.BodyHandlers.discarding() as BodyHandler<Unit>
}