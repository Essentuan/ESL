package net.essentuan.esl.scheduling

import net.essentuan.esl.future.api.Future
import kotlin.coroutines.CoroutineContext

sealed interface TaskScope {
    fun <T> launch(name: String, block: suspend TaskScope.() -> T): Future<T>
}