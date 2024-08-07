package net.essentuan.esl.other

import java.util.Base64

object Base64 {
    fun encode(string: String): String {
        return Base64.getEncoder().encodeToString(string.toByteArray())
    }

    fun decode(string: String): String {
        return String(Base64.getDecoder().decode(string.toByteArray()))
    }
}