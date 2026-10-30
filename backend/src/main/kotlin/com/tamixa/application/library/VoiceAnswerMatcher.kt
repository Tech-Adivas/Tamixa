package com.tamixa.application.library

/**
 * Word-level match of what a child said against the answer labels of a question. Works for any script (Tamil
 * vowel signs are kept). Returns a choice only when one answer clearly wins; otherwise the caller asks the LLM.
 */
object VoiceAnswerMatcher {

    data class Choice(val id: String, val label: String)

    /** At least half the answer's words, and clearly ahead of the next answer. */
    private const val MIN_SCORE = 0.5
    private const val MIN_MARGIN = 0.25
    /** Tamil/Hindi words take suffixes ("மையம்" → "மையத்திற்கு"), so words sharing a long enough start count. */
    private const val MIN_SHARED_PREFIX = 3

    fun normalize(text: String): String =
        text.lowercase()
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), " ")
            .trim()

    private fun words(text: String): List<String> = normalize(text).split(' ').filter { it.isNotBlank() }

    /** The clearly best answer for [heard], or null when none or several fit. */
    fun match(heard: String, choices: List<Choice>): Choice? {
        val heardNorm = normalize(heard)
        if (heardNorm.isBlank() || choices.isEmpty()) return null
        val scored = choices.map { it to score(heardNorm, it.label) }.sortedByDescending { it.second }
        val (best, bestScore) = scored.first()
        val runnerUp = scored.getOrNull(1)?.second ?: 0.0
        return if (bestScore >= MIN_SCORE && bestScore - runnerUp >= MIN_MARGIN) best else null
    }

    /** 0..1: share of the label's words the child said (exact, or same word with a different ending). */
    fun score(heardNormalized: String, label: String): Double {
        val labelNorm = normalize(label)
        if (labelNorm.isBlank()) return 0.0
        if (heardNormalized.contains(labelNorm)) return 1.0
        val labelWords = labelNorm.split(' ').filter { it.isNotBlank() }
        val heardWords = heardNormalized.split(' ').filter { it.isNotBlank() }
        val hits = labelWords.count { lw -> heardWords.any { hw -> sameWord(lw, hw) } }
        return hits.toDouble() / labelWords.size
    }

    private fun sameWord(a: String, b: String): Boolean {
        if (a == b) return true
        val shared = a.commonPrefixWith(b).length
        val shorter = minOf(a.length, b.length)
        return shared >= MIN_SHARED_PREFIX && shared >= (shorter * 0.6)
    }
}
