package net.essentuan.esl.collector

class LinkedSetCollector<T> : SimpleCollector<T, MutableSet<T>, Set<T>>() {
    override fun supply(): MutableSet<T> {
        return LinkedHashSet()
    }

    override fun accumulate(container: MutableSet<T>, value: T) {
        container.add(value)
    }

    override fun combine(left: MutableSet<T>, right: MutableSet<T>): MutableSet<T> {
        left.addAll(right)

        return left
    }

    override fun finish(container: MutableSet<T>): Set<T> {
        return container
    }
}
