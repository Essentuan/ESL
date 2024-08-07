package net.essentuan.esl.other

object Filters {
    inline fun <reified T : Throwable> causedBy(): ((Throwable) -> Boolean) =
        { it.causedBy<T>() }

    inline fun <reified T> instanceOf(): (Any?) -> Boolean =
        { it is T }
}