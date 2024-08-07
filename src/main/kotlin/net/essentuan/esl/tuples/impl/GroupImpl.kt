package net.essentuan.esl.tuples.impl

import net.essentuan.esl.tuples.groups.*

abstract class GroupImpl protected constructor(size: Int, vararg objects: Any?) : TupleImpl(size, *objects), Group {
    override operator fun set(i: Int, `object`: Any?): Any? {
        val old = arr[i]
        arr[i] = `object`!!

        return old
    }

    override fun toString(): String {
        return toString("Group")
    }

    class Arbitrary(vararg objects: Any?) : GroupImpl(objects.size, *objects) {
        override fun first(): Any? {
            return get(0)
        }
    }

    class Mono<T>(vararg objects: Any?) : GroupImpl(1, *objects), MonoGroup<T> {
        override fun first(): T? {
            return super<GroupImpl>.first() as T;
        }
    }

    class Bi<First, Second>(vararg objects: Any?) : GroupImpl(2, *objects), BiGroup<First, Second> {
        override fun first(): First? {
            TODO("Not yet implemented")
        }
    }

    class Tri<First, Second, Third>(vararg objects: Any?) : GroupImpl(3, *objects), TriGroup<First, Second, Third> {
        override fun first(): First? {
            TODO("Not yet implemented")
        }
    }

    class Quad<First, Second, Third, Fourth>(vararg objects: Any?) : GroupImpl(4, *objects),
        QuadGroup<First, Second, Third, Fourth> {
        override fun first(): First? {
            TODO("Not yet implemented")
        }
    }

    class Quint<First, Second, Third, Fourth, Fifth>(vararg objects: Any?) : GroupImpl(5, *objects),
        QuintGroup<First, Second, Third, Fourth, Fifth> {
        override fun first(): First? {
            TODO("Not yet implemented")
        }
    }

    class Sex<First, Second, Third, Fourth, Fifth, Sixth>(vararg objects: Any?) : GroupImpl(6, *objects),
        SexGroup<First, Second, Third, Fourth, Fifth, Sixth> {
        override fun first(): First? {
            TODO("Not yet implemented")
        }
    }
}
