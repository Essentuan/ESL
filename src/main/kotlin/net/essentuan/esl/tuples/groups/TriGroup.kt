package net.essentuan.esl.tuples.groups

import net.essentuan.esl.tuples.TriTuple

interface TriGroup<First, Second, Third> : TriTuple<First, Second, Third>, BiGroup<First, Second>, Group {
    fun setThird(value: Third?): Third? {
        return set(2, value) as Third
    }
}
