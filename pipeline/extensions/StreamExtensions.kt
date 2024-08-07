package net.essentuan.esl.async.pipeline.extensions

import net.essentuan.esl.async.pipeline.Pipeline
import reactor.core.publisher.Flux
import java.util.stream.Stream

fun <T> Stream<T>.pipe(): Pipeline<T> = Pipeline.of(this)

fun <T> Stream<T>.flux(): Flux<T> = Flux.fromStream(this)