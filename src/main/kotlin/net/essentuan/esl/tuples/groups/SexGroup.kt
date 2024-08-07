package net.essentuan.esl.tuples.groups

import net.essentuan.esl.tuples.SexTuple

interface SexGroup<First, Second, Third, Fourth, Fifth, Sixth> : SexTuple<First, Second, Third, Fourth, Fifth, Sixth>,
    QuintGroup<First, Second, Third, Fourth, Fifth>, Group {
    fun setSixth(value: Sixth?): Sixth? {
        return set(5, value) as Sixth
    }
}
