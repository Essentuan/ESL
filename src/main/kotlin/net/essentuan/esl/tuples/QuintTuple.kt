package net.essentuan.esl.tuples

interface QuintTuple<First, Second, Third, Fourth, Fifth> : QuadTuple<First, Second, Third, Fourth> {
    fun fifth(): Fifth? {
        return get(4) as Fifth
    }
}
