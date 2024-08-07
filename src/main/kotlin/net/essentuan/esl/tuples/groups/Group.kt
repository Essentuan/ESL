package net.essentuan.esl.tuples.groups

import net.essentuan.esl.tuples.Tuple
import net.essentuan.esl.tuples.impl.GroupImpl

interface Group : Tuple {
    operator fun set(i: Int, `object`: Any?): Any?

    companion object {
        fun <T> of(value: T): MonoGroup<T> {
            return GroupImpl.Mono(value)
        }

        fun <First, Second> of(first: First, second: Second): BiGroup<First, Second> {
            return GroupImpl.Bi(first, second)
        }

        fun <First, Second, Third> of(first: First, second: Second, third: Third): TriGroup<First, Second, Third> {
            return GroupImpl.Tri(first, second, third)
        }

        fun <First, Second, Third, Fourth> of(
            first: First,
            second: Second,
            third: Third,
            fourth: Fourth
        ): QuadGroup<First, Second, Third, Fourth> {
            return GroupImpl.Quad(first, second, third, fourth)
        }

        fun <First, Second, Third, Fourth, Fifth> of(
            first: First,
            second: Second,
            third: Third,
            fourth: Fourth,
            fifth: Fifth
        ): QuintGroup<First, Second, Third, Fourth, Fifth> {
            return GroupImpl.Quint(first, second, third, fourth, fifth)
        }

        fun <First, Second, Third, Fourth, Fifth, Sixth> of(
            first: First,
            second: Second,
            third: Third,
            fourth: Fourth,
            fifth: Fifth,
            sixth: Sixth
        ): SexGroup<First, Second, Third, Fourth, Fifth, Sixth> {
            return GroupImpl.Sex(first, second, third, fourth, fifth, sixth)
        }

        fun of(vararg objects: Any?): Group {
            return GroupImpl.Arbitrary(objects)
        }
    }
}
