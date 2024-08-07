package net.essentuan.esl.tuples

import net.essentuan.esl.other.unsupported
import net.essentuan.esl.tuples.impl.TupleImpl

interface Tuple : MutableCollection<Any?> {
    operator fun get(i: Int): Any?

    operator fun <T> get(i: Int, cls: Class<T>): T?

    fun first(): Any?

    fun last(): Any?

    override fun add(obj: Any?): Boolean {
        unsupported()
    }

    override fun remove(o: Any?): Boolean {
        unsupported()
    }

    override fun addAll(c: Collection<*>): Boolean {
        unsupported()
    }

    override fun retainAll(c: Collection<*>): Boolean {
        unsupported()
    }

    override fun removeAll(c: Collection<*>): Boolean {
        unsupported()
    }

    override fun clear() {
        unsupported()
    }

    companion object {
        fun <T> of(value: T): MonoTuple<T>? {
            return TupleImpl.Mono(value)
        }

        fun <First, Second> of(first: First, second: Second): BiTuple<First, Second>? {
            return TupleImpl.Bi(first, second)
        }

        fun <First, Second, Third> of(first: First, second: Second, third: Third): TriTuple<First, Second, Third>? {
            return TupleImpl.Tri(first, second, third)
        }

        fun <First, Second, Third, Fourth> of(
            first: First,
            second: Second,
            third: Third,
            fourth: Fourth
        ): QuadTuple<First, Second, Third, Fourth> {
            return TupleImpl.Quad(first, second, third, fourth)
        }

        fun <First, Second, Third, Fourth, Fifth> of(
            first: First,
            second: Second,
            third: Third,
            fourth: Fourth,
            fifth: Fifth
        ): QuintTuple<First, Second, Third, Fourth, Fifth> {
            return TupleImpl.Quint(first, second, third, fourth, fifth)
        }

        fun <First, Second, Third, Fourth, Fifth, Sixth> of(
            first: First,
            second: Second,
            third: Third,
            fourth: Fourth,
            fifth: Fifth,
            sixth: Sixth
        ): SexTuple<First, Second, Third, Fourth, Fifth, Sixth> {
            return TupleImpl.Sex(first, second, third, fourth, fifth, sixth)
        }

        fun of(vararg objects: Any?): Tuple {
            return TupleImpl.Arbitrary(objects)
        }
    }
}
