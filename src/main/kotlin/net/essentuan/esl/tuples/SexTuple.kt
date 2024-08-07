package net.essentuan.esl.tuples

interface SexTuple<First, Second, Third, Fourth, Fifth, Sixth> : QuintTuple<First, Second, Third, Fourth, Fifth> {
    fun sixth(): Sixth? {
        return get(5) as Sixth
    }
}
