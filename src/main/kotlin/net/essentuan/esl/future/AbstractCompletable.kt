package net.essentuan.esl.future

import net.essentuan.esl.future.api.Completable
import net.essentuan.esl.future.api.Future

abstract class AbstractCompletable<T> : AbstractFuture<T>(), Completable<T> {
    override fun complete(value: T) {
        super<AbstractFuture>.complete(value)
    }

    override fun raise(ex: Throwable) {
        super<AbstractFuture>.raise(ex)
    }

    override fun from(other: Future<T>): Completable<T> {
        copy(other)

        return this
    }

    companion object {
        val COMPLETED: Completable<Unit> = Completable()

        init {
            COMPLETED.complete(Unit)
        }
    }
}
