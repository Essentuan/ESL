package net.essentuan.esl.fetch

object Fetch

val NOTHING: String = ""

inline fun <T> fetch(block: Fetch.() -> T): T = block(Fetch)