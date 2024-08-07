package net.essentuan.esl.tuples.groups

import net.essentuan.esl.tuples.QuadTuple

interface QuadGroup<First, Second, Third, Fourth> : QuadTuple<First, Second, Third, Fourth>,
    TriGroup<First, Second, Third>, Group {
    fun setFourth(value: Fourth?): Fourth? {
        return set(3, value) as Fourth
    }
}
