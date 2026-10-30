package com.tamixa.application.narration

/**
 * Shared utilities for narration text. Used as defense-in-depth to strip TTS markers
 * before sending to providers that may receive text that bypassed SSML processing
 * (e.g. SSML-to-plain extraction, cloned voice synthesis).
 */
object NarrationTextUtils {

    /** Pause markers: [Pause 500ms], [Pause 1s], [Pause 1.5s]; ASCII or fullwidth brackets. */
    private val PAUSE_REGEX = Regex("""[\[\［]\s*Pause\s*[\d.]+(?:ms|s)\s*[\]\］]""", RegexOption.IGNORE_CASE)

    /** Tone/scene markers used by prompt templates (e.g. [Warm tone], [Scene shifts], [Brisk pacing]). */
    private val TONE_REGEX = Regex(
        """[\[\［]\s*(?:Warm\s*tone|Gentle\s*tone|Calm(?:\s*tone)?|Happy\s*tone|Excited\s*tone|Playful\s*tone|Curious\s*tone|Wonder\s*tone|Reassuring\s*tone|Thoughtful\s*tone|Soft\s*voice|Whisper(?:ed)?\s*tone|Emotional\s*tone|Soft\s*emotional\s*tone|Celebration\s*tone|Storyteller\s*tone|Slow\s*pacing|(?:Meduim|Medium)\s*pacing|Brisk\s*pacing|Scene\s*opens\s*softly|Scene\s*shifts|A\s*gentle\s*moment|A\s*magical\s*moment|A\s*quiet\s*pause|A\s*joyful\s*moment|A\s*surprise\s*moment|Closing\s*tone|Audio\s*imagination|Joyful\s*moment|Clear\s*tone)\s*[\]\］]""",
        RegexOption.IGNORE_CASE
    )

    /** Fallback: any bracketed phrase containing marker keywords (catches translated/variant forms). */
    private val FALLBACK_REGEX = Regex(
        """[\[\［][^\]\］]*(?:tone|voice|pacing|scene|calm|whisper|excited|pause|happy|warm|soft|storyteller|gentle|curious|wonder|reassuring|thoughtful|celebration|audio|closing)[^\]\］]*[\]\］]""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Strips narration markers ([Pause 500ms], [Warm tone], etc.) from plain text.
     * Call before sending text to TTS providers that receive raw text (OpenAI, cloned voice, etc.).
     */
    fun stripRemainingMarkers(text: String): String {
        if (text.isBlank()) return ""
        return text
            .replace(PAUSE_REGEX, " ")
            .replace(TONE_REGEX, "")
            .replace(FALLBACK_REGEX, "")
            .replace(Regex("""\bnn\b""", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("""[^\S\n]{2,}"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }

    /** After a sentence end: . ! ? (Latin and South Indian scripts), । ॥ (Hindi), … */
    private val SENTENCE_END = Regex("(?<=[.!?\u0964\u0965\u2026])\\s+")
    private val PARAGRAPH_BREAK = Regex("\\n\\s*\\n")
    private val WHITESPACE = Regex("\\s+")
    private const val PARAGRAPH_SEPARATOR = "\n\n"

    /**
     * Splits narration text into parts of at most [maxChars] for TTS providers with a per-request limit.
     * Paragraphs (separated by a blank line) stay separated by a blank line inside a part, so the voice can pause
     * there; other whitespace is collapsed. A part ends at a paragraph end where possible, else at a sentence end
     * (a longer sentence is split at spaces, a single longer word at [maxChars]). No text is lost.
     */
    fun splitForSpeech(text: String, maxChars: Int): List<String> {
        require(maxChars > 0) { "maxChars must be positive" }
        val paragraphs = text.split(PARAGRAPH_BREAK).map { it.replace(WHITESPACE, " ").trim() }.filter { it.isNotEmpty() }
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        for (paragraph in paragraphs) {
            var separator = PARAGRAPH_SEPARATOR
            for (piece in piecesOf(paragraph, maxChars)) {
                if (current.isNotEmpty() && current.length + separator.length + piece.length > maxChars) {
                    parts += current.toString()
                    current.clear()
                }
                if (current.isNotEmpty()) current.append(separator)
                current.append(piece)
                separator = " "
            }
        }
        if (current.isNotEmpty()) parts += current.toString()
        return parts
    }

    /** [paragraph] whole when it fits, else its sentences (and words of an over-long sentence) packed to [maxChars]. */
    private fun piecesOf(paragraph: String, maxChars: Int): List<String> {
        if (paragraph.length <= maxChars) return listOf(paragraph)
        return paragraph.split(SENTENCE_END).flatMap { sentence ->
            if (sentence.length <= maxChars) listOf(sentence)
            else packWords(sentence.split(' ').flatMap { it.chunked(maxChars) }, maxChars)
        }
    }

    private fun packWords(words: List<String>, maxChars: Int): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        for (word in words) {
            if (current.isNotEmpty() && current.length + 1 + word.length > maxChars) {
                out += current.toString()
                current.clear()
            }
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        }
        if (current.isNotEmpty()) out += current.toString()
        return out
    }

    /**
     * Plain text for TTS providers that don't read SSML (OpenAI, ElevenLabs). Each line of the narration SSML is a
     * segment (they end with a <break/> and a newline); they stay paragraphs (blank line between) so the voice pauses
     * there. Markup, entities and narration markers are removed.
     */
    fun ssmlToSpeechText(ssml: String): String {
        val plain = ssml
            .replace(Regex("<break[^>]*/>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .lines()
            .map { it.replace(WHITESPACE, " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString(PARAGRAPH_SEPARATOR)
        val cleaned = stripRemainingMarkers(plain)
        return cleaned.ifBlank { plain }.ifBlank { ssml }
    }
}
