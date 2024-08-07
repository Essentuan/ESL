package net.essentuan.esl.tuples

interface TriTuple<First, Second, Third> : BiTuple<First, Second> {
    fun third(): Third? {
        return get(2) as Third
    }
}

