package com.tamixa.application.narration

/**
 * Enforces a maximum word count for pipeline translation/rewrite text.
 * Truncates at the last sentence end before [maxWords] when possible so TTS does not cut mid-thought.
 */
object StoryPipelineWordLimit {

    fun clampToMaxWords(text: String, maxWords: Int): String {
        if (maxWords <= 0 || text.isBlank()) return text
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size <= maxWords) return text
        val truncated = words.take(maxWords).joinToString(" ")
        val lastSentenceEnd = listOf(
            truncated.lastIndexOf('.'),
            truncated.lastIndexOf('!'),
            truncated.lastIndexOf('?'),
            truncated.lastIndexOf('।') // Devanagari danda
        ).maxOrNull() ?: -1
        return if (lastSentenceEnd >= truncated.length / 2) {
            truncated.take(lastSentenceEnd + 1).trim()
        } else {
            truncated
        }
    }
}
