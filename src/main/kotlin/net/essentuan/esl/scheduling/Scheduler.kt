package net.essentuan.esl.scheduling

import net.essentuan.esl.collections.mutableSetFrom
import net.essentuan.esl.other.Output
import net.essentuan.esl.scheduling.groups.StaticTask
import java.util.concurrent.ConcurrentHashMap


object Scheduler : MutableSet<Task.Group> by mutableSetFrom(::ConcurrentHashMap), Output.Group() {
    var capactiy: Int
        set(value) {
            TaskDispatcher.capacity = value
        }
        get() = TaskDispatcher.capacity


    fun start() {
        TaskDispatcher.start()

        add(StaticTask)
    }
}