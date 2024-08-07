package net.essentuan.esl.other

import org.apache.logging.log4j.Logger

private typealias SLogger = org.slf4j.Logger

interface Output {
    fun info(message: String, throwable: Throwable? = null)

    fun warn(message: String, throwable: Throwable? = null)

    fun error(message: String, throwable: Throwable? = null)

    fun fatal(message: String, throwable: Throwable? = null)

    abstract class Group : Output {
        private val outputs = mutableListOf<Output>()

        operator fun plusAssign(output: Output) {
            outputs += output
        }

        operator fun plusAssign(logger: Logger) {
            outputs += logger.output()
        }

        operator fun plusAssign(logger: SLogger) {
            outputs += logger.output()
        }

        private fun log(level: Output.(String, Throwable?) -> Unit, message: String, throwable: Throwable?) {
            for (output in outputs)
                output.level(message, throwable)
        }

        override fun info(message: String, throwable: Throwable?) = log(Output::info, message, throwable)

        override fun warn(message: String, throwable: Throwable?) = log(Output::warn, message, throwable)

        override fun error(message: String, throwable: Throwable?) = log(Output::error, message, throwable)

        override fun fatal(message: String, throwable: Throwable?) = log(Output::fatal, message, throwable)
    }
}

fun Logger.output(): Output = object : Output {
    override fun info(message: String, throwable: Throwable?) {
        if (throwable == null)
            this@output.info(message)
        else
            this@output.info(message, throwable)
    }

    override fun warn(message: String, throwable: Throwable?) {
        if (throwable == null)
            this@output.warn(message)
        else
            this@output.warn(message, throwable)
    }

    override fun error(message: String, throwable: Throwable?) {
        if (throwable == null)
            this@output.error(message)
        else
            this@output.error(message, throwable)
    }

    override fun fatal(message: String, throwable: Throwable?) {
        if (throwable == null)
            this@output.fatal(message)
        else
            this@output.fatal(message, throwable)
    }
}

fun SLogger.output(): Output = object : Output {
    override fun info(message: String, throwable: Throwable?) {
        if (throwable == null)
            this@output.info(message)
        else
            this@output.info(message, throwable)
    }

    override fun warn(message: String, throwable: Throwable?) {
        if (throwable == null)
            this@output.warn(message)
        else
            this@output.warn(message, throwable)
    }

    override fun error(message: String, throwable: Throwable?) {
        if (throwable == null)
            this@output.error(message)
        else
            this@output.error(message, throwable)
    }

    override fun fatal(message: String, throwable: Throwable?) = this.error(message, throwable)
}