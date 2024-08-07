package net.essentuan.esl.other

typealias static = JvmStatic

inline fun unsupported(): Nothing = throw UnsupportedOperationException()

inline fun unsupported(message: String): Nothing = throw UnsupportedOperationException(message)

inline fun unsupported(lazyMessage: () -> Any): Nothing = throw UnsupportedOperationException(lazyMessage().toString())

inline fun <reified T : Throwable> Throwable.causedBy(predicate: (T) -> Boolean = { true }): Boolean {
    when(this) {
        is T -> return true
        else -> {
            var cause = this.cause
            while (cause != null) {
                if (cause is T && predicate(cause))
                    return true

                cause = cause.cause
            }
        }
    }

    return false
}