package net.essentuan.esl.json.type

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GsonTypes {
    val DEFAULT: Gson = GsonBuilder().create()
    val PRETTY_PRINTING: Gson = GsonBuilder().setPrettyPrinting().create()
}