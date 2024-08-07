package net.essentuan.esl.other

inline infix fun <T: Any> T?.elif(block: () -> T): T = this ?: block()

@Throws(RuntimeException::class)
fun error(e: Throwable?): Nothing {
    when (e) {
        is ReflectiveOperationException -> error(e.cause)
        is RuntimeException -> throw e
        else -> error("")
    }
}

fun stacktrace(): Array<StackTraceElement> = thread().stackTrace

fun thread(): Thread = Thread.currentThread()

inline fun <T: Any, U> T.lock(init: T.() -> U): U = synchronized(this) { init(this) }