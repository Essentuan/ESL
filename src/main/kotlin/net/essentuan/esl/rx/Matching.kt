package net.essentuan.esl.rx

enum class Matching(
    val stopOnPredicateMatches: Boolean,
    val failResult: Boolean
) {
    ANY(true, true),
    ALL(false, false),
    NONE(true, false)
}