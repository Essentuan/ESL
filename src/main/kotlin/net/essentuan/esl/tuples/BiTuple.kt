package net.essentuan.esl.tuples

interface BiTuple<First, Second> : MonoTuple<First> {
    fun second(): Second? {
        return get(1) as Second
    }
}
