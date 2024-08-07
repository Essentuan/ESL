package net.essentuan.esl.tuples.groups

import net.essentuan.esl.tuples.MonoTuple

interface MonoGroup<T> : MonoTuple<T>, Group {
    fun setFirst(value: T?): T? {
        return set(0, value) as T
    }
}
