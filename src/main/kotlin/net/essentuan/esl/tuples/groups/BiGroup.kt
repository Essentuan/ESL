package net.essentuan.esl.tuples.groups

import net.essentuan.esl.tuples.BiTuple

interface BiGroup<First, Second> : MutableMap.MutableEntry<First?, Second?>, BiTuple<First, Second>, MonoGroup<First>,
    Group {
    fun setSecond(value: Second?): Second? {
        return set(1, value) as Second
    }

    override val key: First?
        get() = first()

    override val value: Second?
        get() = second()

    override fun setValue(newValue: Second?): Second? {
        return setSecond(value)
    }
}
