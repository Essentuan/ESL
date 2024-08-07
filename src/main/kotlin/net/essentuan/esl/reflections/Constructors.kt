package net.essentuan.esl.reflections

import net.essentuan.esl.other.stacktrace
import net.essentuan.esl.reflections.extensions.extends
import kotlin.reflect.KClass

object Constructors {
    fun trace(): KClass<*> {
        val stacktrace = stacktrace()

        if (stacktrace[3].methodName != "<init>")
            throw IllegalStateException("Cannot trace caller class outside constructor!")

        val start = Class.forName(stacktrace[3].className)
        var previous: Class<*> = start

        for (i in 4..<stacktrace.size) {
            val element = stacktrace[i]

            if (element.methodName != "<init>")
                break

            val cls = Class.forName(element.className)

            if (!cls.extends(start))
                break

            previous = cls
        }

        return previous.kotlin
    }
}