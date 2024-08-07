package net.essentuan.esl.iteration

abstract class SimpleIterator<T> : Iterator<T> {
    protected abstract fun compute(): T

    override fun next(): T {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        return compute()
    }

    companion object {
        val EMPTY: SimpleIterator<Any?> = object : SimpleIterator<Any?>() {
            override fun compute(): Any? {
                return null;
            }

            override fun hasNext(): Boolean {
                return false
            }
        }
    }
}
