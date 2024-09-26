package net.essentuan.esl.scheduling.impl

import net.essentuan.esl.scheduling.Scheduler
import net.essentuan.esl.scheduling.Task
import java.util.concurrent.ConcurrentHashMap

abstract class GroupImpl(override val name: String) : Task.Group {
    private val tasks = ConcurrentHashMap<String, Task>()

    override val size: Int
        get() = tasks.size

    override fun isEmpty(): Boolean =
        tasks.isEmpty()

    override fun plusAssign(task: Task) {
        tasks.putIfAbsent(task.id, task)
    }

    override fun get(id: String): Task? =
        tasks[id]

    override fun contains(id: String): Boolean {
        return tasks.containsKey(id)
    }

    override fun containsAll(elements: Collection<Task>): Boolean {
        for (e in elements)
            if (e !in this)
                return false

        return true
    }

    override fun minusAssign(task: Task) {
        tasks.remove(task.id, task)
    }

    override fun resume() {
        for (task in this)
            task.resume()
    }

    override fun suspend(cancel: Boolean) {
        for (task in this)
            task.suspend(cancel)
    }

    override fun iterator(): Iterator<Task> =
        tasks.values.iterator()

    override fun close() {
        Scheduler.remove(this)
    }

}