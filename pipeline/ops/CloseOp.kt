package net.essentuan.esl.async.pipeline.ops

import net.essentuan.esl.async.pipeline.Gatherer

class CloseOp<T>(private val handler: Runnable) : Gatherer<T, T>() {
    override fun onCancel() {
        handler.run()
    }

    override fun onComplete() {
        handler.run()

        complete()
    }
}
