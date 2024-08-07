package net.essentuan.esl.async.pipeline.subscribers

import net.essentuan.esl.future.AbstractFuture
import net.essentuan.esl.async.pipeline.extensions.pipe
import org.reactivestreams.Publisher

open class SingletonSubscriber<T>(publisher: Publisher<T>) : AbstractFuture<T>() {
    init {
        copy { publisher.pipe().first() }
    }
}
