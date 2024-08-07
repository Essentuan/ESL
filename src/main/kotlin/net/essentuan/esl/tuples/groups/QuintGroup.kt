package net.essentuan.esl.tuples.groups

import net.essentuan.esl.tuples.QuintTuple

interface QuintGroup<First, Second, Third, Fourth, Fifth> : QuintTuple<First, Second, Third, Fourth, Fifth>,
    QuadGroup<First, Second, Third, Fourth>, Group {
    fun setFifth(value: Fifth?): Fifth? {
        return set(4, value) as Fifth
    }
}