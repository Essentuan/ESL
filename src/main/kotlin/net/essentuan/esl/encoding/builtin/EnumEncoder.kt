package net.essentuan.esl.encoding.builtin

import net.essentuan.esl.encoding.StringBasedEncoder
import net.essentuan.esl.reflections.Annotations
import net.essentuan.esl.reflections.extensions.simpleString
import net.essentuan.esl.string.extensions.bestMatch
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Type
import java.util.regex.Pattern

object EnumEncoder : StringBasedEncoder<Enum<*>>() {
    val EMPTY_PATTERN: Pattern = Pattern.compile("[_ -']")
    val cache: MutableMap<String, Enum<*>> = mutableMapOf()

    override fun encode(obj: Enum<*>, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): String =
        obj.name

    override fun decode(obj: String, flags: Set<Any>, type: Class<*>, element: AnnotatedElement, vararg typeArgs: Type): Enum<*> =
        cache.computeIfAbsent(obj) {
            (fix(obj).bestMatch(
                type.enumConstants.asSequence(),
                { arrayOf(fix(it)) }
            ) ?: throw IllegalArgumentException("$obj does not map to ${type.simpleString()}"))  as Enum<*>
        }

    private fun fix(obj: Any): String {
        return EMPTY_PATTERN.matcher(obj.toString()).replaceAll("")
    }
}

inline fun <reified T: Enum<T>> enumValueOf(name: String) =
    EnumEncoder.decode(name, emptySet(), T::class.java, Annotations.empty()) as T