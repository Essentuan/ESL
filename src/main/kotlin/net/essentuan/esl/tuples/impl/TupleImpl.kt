package net.essentuan.esl.tuples.impl

import net.essentuan.esl.iteration.extensions.immutable
import net.essentuan.esl.tuples.BiTuple
import net.essentuan.esl.tuples.MonoTuple
import net.essentuan.esl.tuples.QuadTuple
import net.essentuan.esl.tuples.QuintTuple
import net.essentuan.esl.tuples.SexTuple
import net.essentuan.esl.tuples.TriTuple
import net.essentuan.esl.tuples.Tuple
import java.util.Arrays

@Suppress("UNCHECKED_CAST")
abstract class TupleImpl protected constructor(override val size: Int, vararg objects: Any?) : Tuple {
    protected val arr: Array<Any?> = arrayOf(objects.copyOf())

    override operator fun get(i: Int): Any? {
        return arr[i]
    }

    override operator fun <T> get(i: Int, cls: Class<T>): T? {
        return cls.cast(get(i))
    }

    override fun first(): Any? {
        return if (isEmpty()) null else this[0]
    }

    override fun last(): Any? {
        return if (isEmpty()) null else this[size - 1]
    }

    override fun containsAll(elements: Collection<Any?>): Boolean {
        return setOf(arr).containsAll(elements)
    }

    override fun contains(element: Any?): Boolean {
        for (`object` in this) if (`object` == element) return true

        return false
    }

    override fun iterator(): MutableIterator<Any?> {
        return arr.iterator().immutable()
    }

    fun toArray(): Array<Any?> {
        return arr.copyOf(size)
    }

    fun <T> toArray(a: Array<T?>): Array<T?> {
        if (a.size < size) return Arrays.copyOf<Any?, Any>(arr, size, a.javaClass) as Array<T?>

        System.arraycopy(arr, 0, a, 0, size)

        if (a.size > size) a[size] = null

        return a
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true

        if (other is TupleImpl) return arr.contentDeepEquals(other.arr)

        if (other !is Tuple) return false

        if (other.size !== size) return false

        for (i in 0 until size) if (get(i) != other[i]) return false

        return true
    }

    override fun hashCode(): Int {
        return arr.contentDeepHashCode()
    }

    override fun isEmpty(): Boolean = size == 0

    protected fun toString(prefix: String): String {
        val result = arr.contentDeepToString()

        return prefix + (if (result == "null") "[null]" else result)
    }

    override fun toString(): String {
        return toString("Tuple")
    }

    class Arbitrary(vararg objects: Any?) : TupleImpl(objects.size, *objects)

    class Mono<T>(vararg objects: Any?) : TupleImpl(1, *objects), MonoTuple<T> {
        override fun first(): T? {
            return super<TupleImpl>.first() as T?
        }
    }

    class Bi<First, Second>(vararg objects: Any?) : TupleImpl(2, *objects), BiTuple<First, Second> {
        override fun first(): First? {
            return super<TupleImpl>.first() as First?
        }
    }

    class Tri<First, Second, Third>(vararg objects: Any?) : TupleImpl(3, *objects), TriTuple<First, Second, Third> {
        override fun first(): First? {
            return super<TupleImpl>.first() as First?
        }
    }

    class Quad<First, Second, Third, Fourth>(vararg objects: Any?) : TupleImpl(4, *objects),
        QuadTuple<First, Second, Third, Fourth> {
        override fun first(): First? {
            return super<TupleImpl>.first() as First?
        }
    }

    class Quint<First, Second, Third, Fourth, Fifth>(vararg objects: Any?) : TupleImpl(5, *objects),
        QuintTuple<First, Second, Third, Fourth, Fifth> {
        override fun first(): First? {
            return super<TupleImpl>.first() as First?
        }
    }

    class Sex<First, Second, Third, Fourth, Fifth, Sixth>(vararg objects: Any?) : TupleImpl(6, *objects),
        SexTuple<First, Second, Third, Fourth, Fifth, Sixth> {
        override fun first(): First? {
            return super<TupleImpl>.first() as First?
        }
    }
}
