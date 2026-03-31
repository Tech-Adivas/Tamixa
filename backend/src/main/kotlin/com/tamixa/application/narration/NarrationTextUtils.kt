package com.tamixa.application.narration

/**
 * Shared utilities for narration text. Used as defense-in-depth to strip TTS markers
 * before sending to providers that may receive text that bypassed SSML processing
 * (e.g. SSML-to-plain extraction, cloned voice synthesis).
 */
object NarrationTextUtils {

    /** Pause markers: [Pause 500ms], [Pause 1s], [Pause1s], etc. */
    private val PAUSE_REGEX = Regex("""\[Pause\s*\d+(?:ms|s)\s*\]""", RegexOption.IGNORE_CASE)

    /** Tone/scene markers used by prompt templates (e.g. [Warm tone], [Scene shifts], [Brisk pacing]). */
    private val TONE_REGEX = Regex(
        """[\[\［]\s*(?:Warm\s*tone|Gentle\s*tone|Calm(?:\s*tone)?|Happy\s*tone|Excited\s*tone|Playful\s*tone|Curious\s*tone|Wonder\s*tone|Reassuring\s*tone|Thoughtful\s*tone|Soft\s*voice|Whisper(?:ed)?\s*tone|Emotional\s*tone|Soft\s*emotional\s*tone|Celebration\s*tone|Storyteller\s*tone|Slow\s*pacing|Medium\s*pacing|Brisk\s*pacing|Scene\s*opens\s*softly|Scene\s*shifts|A\s*gentle\s*moment|A\s*magical\s*moment|A\s*quiet\s*pause|A\s*joyful\s*moment|A\s*surprise\s*moment|Closing\s*tone|Audio\s*imagination|Joyful\s*moment|Clear\s*tone)\s*[\]\］]""",
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
}
