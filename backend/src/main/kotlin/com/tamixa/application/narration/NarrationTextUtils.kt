package com.tamixa.application.narration

/**
 * Shared utilities for narration text. Used as defense-in-depth to strip TTS markers
 * before sending to providers that may receive text that bypassed SSML processing
 * (e.g. SSML-to-plain extraction, cloned voice synthesis).
 */
object NarrationTextUtils {

    /** Pause markers: [Pause 500ms], [Pause 1s], [Pause1s], etc. */
    private val PAUSE_REGEX = Regex("""\[Pause\s*\d+(?:ms|s)\s*\]""", RegexOption.IGNORE_CASE)

    /** Tone markers: [Warm tone], [Happy tone], [Soft voice], etc. */
    private val TONE_REGEX = Regex(
        """[\[\［]\s*(?:Happy\s*tone|Warm\s*tone|Soft\s*voice|Calm|Whisper|Excited)\s*[\]\］]""",
        RegexOption.IGNORE_CASE
    )

    /** Fallback: any bracketed phrase containing marker keywords (catches translated/variant forms). */
    private val FALLBACK_REGEX = Regex(
        """[\[\［][^\]\］]*(?:tone|voice|calm|whisper|excited|pause|happy|warm|soft)[^\]\］]*[\]\］]""",
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
            .replace(Regex("""[^\S\n]{2,}"""), " ")
            .trim()
    }
}
