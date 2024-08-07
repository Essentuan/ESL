package net.essentuan.esl.tuples

interface MonoTuple<T> : Tuple {
    override fun first(): T? {
        return get(0) as T
    }
}

