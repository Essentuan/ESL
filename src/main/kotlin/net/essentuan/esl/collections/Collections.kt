package net.essentuan.esl.collections

import java.util.Collections
import java.util.EnumMap

fun <K, V> MutableMap<K, V>.synchronized(): MutableMap<K, V> = Collections.synchronizedMap(this)

fun <T> MutableMap<T, Boolean>.setOf(): MutableSet<T> = Collections.newSetFromMap(this)

fun <T> MutableCollection<T>.synchronized(): MutableCollection<T> = Collections.synchronizedCollection(this)

fun <T> MutableList<T>.synchronized(): MutableList<T> = Collections.synchronizedList(this)

fun <T> MutableSet<T>.synchronized(): MutableSet<T> = Collections.synchronizedSet(this)

inline fun <T> mutableSetFrom(map: () -> MutableMap<T, Boolean>): MutableSet<T> = Collections.newSetFromMap(map())

inline fun <reified K : Enum<K>, V> enumMapOf(): EnumMap<K, V>
    = EnumMap<K, V>(K::class.java)

inline fun <reified K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>): EnumMap<K, V>
        = EnumMap<K, V>(K::class.java).also { it.putAll(pairs) }