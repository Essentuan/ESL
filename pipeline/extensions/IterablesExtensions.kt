package net.essentuan.esl.async.pipeline.extensions

import net.essentuan.esl.async.pipeline.Pipeline

fun <T> Iterable<T>.pipe(): Pipeline<T> = Pipeline.of(this)
