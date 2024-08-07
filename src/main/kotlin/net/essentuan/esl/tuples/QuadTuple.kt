package net.essentuan.esl.tuples

interface QuadTuple<First, Second, Third, Fourth> : TriTuple<First, Second, Third> {
    fun fourth(): Fourth? {
        return get(3) as Fourth
    }
}
