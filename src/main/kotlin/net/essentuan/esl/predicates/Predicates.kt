package net.essentuan.esl.predicates

import net.essentuan.esl.reflections.extensions.extends
import net.essentuan.esl.reflections.extensions.instanceof
import java.util.function.Predicate
import kotlin.reflect.KClass

object Predicates {
    fun <T> instanceOf(cls: Class<*>): Predicate<T> {
        return Predicate { o: T ->
            when (o) {
                is KClass<*> -> o extends cls
                is Class<*> -> o extends cls
                else -> o instanceof cls
            }
        }
    }

    fun <T> instanceOf(cls: KClass<*>): Predicate<T> = instanceOf(cls.java)
}