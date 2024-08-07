package net.essentuan.esl.scheduling

import net.essentuan.esl.iteration.Iterators
import net.essentuan.esl.scheduling.api.Task
import java.lang.reflect.Method
import kotlin.reflect.KFunction

internal class EmptyGroup(
    override val name: String
) : Task.Group {
    override fun get(uid: String): Task? = null

    override fun get(method: Method): Task? = null

    override fun get(func: KFunction<*>): Task? = null

    override fun contains(uid: String): Boolean = false

    override fun contains(method: Method): Boolean = false

    override fun contains(func: KFunction<*>): Boolean = false

    override fun contains(element: Task): Boolean = false

    override fun resume() = Unit
    override fun suspend(cancel: Boolean) = Unit

    override val size: Int = 0

    override fun containsAll(elements: Collection<Task>): Boolean = elements.isEmpty()

    override fun isEmpty(): Boolean = true

    override fun iterator(): Iterator<Task> = Iterators.empty()
    override fun close() = Unit
}