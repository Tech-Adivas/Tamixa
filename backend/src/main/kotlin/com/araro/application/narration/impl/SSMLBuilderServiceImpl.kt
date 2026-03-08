package com.araro.application.narration.impl

import com.araro.application.narration.SSMLBuilderService
import com.araro.domain.narration.EmotionSegment
import com.araro.domain.narration.EmotionTag
import com.araro.domain.narration.EmotionTaggedScript
import com.araro.domain.narration.ToneMode
import org.springframework.stereotype.Service

/**
 * Builds SSML from plain narration text.
 *
 * Architecture: Language-based rate strategy (Indic langs slower), age-based break timing
 * (younger = longer pauses), tone-based rate. Ensures valid SSML via XML escaping.
 *
 * Emotional layer: buildSSMLFromEmotionTagged maps emotion tags to prosody.
 * Deterministic first: no AI-generated content; only prosody attributes.
 */
@Service
class SSMLBuilderServiceImpl : SSMLBuilderService {

    override fun buildSSML(
        scriptText: String,
        language: String,
        age: Int,
        toneMode: ToneMode
    ): String {
        val langAttr = mapLanguageToBCP47(language)
        val rate = resolveRate(language, age, toneMode)
        val breakMs = resolveBreakTimeMs(language, age)
        val breakTag = """<break time="${breakMs}ms"/>"""
        val paragraphs = scriptText.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
        val innerContent = paragraphs.map { escapeXml(it) }
            .joinToString("$breakTag\n") { "<p>$it</p>" }

        val ssml = """
            <speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis">
                <lang xml:lang="$langAttr">
                    <prosody rate="$rate">
                        $innerContent
                    </prosody>
                </lang>
            </speak>
        """.trimIndent()
        return validateAndSanitizeSsml(ssml)
    }

    override fun buildSSMLFromEmotionTagged(
        emotionTagged: EmotionTaggedScript,
        language: String,
        age: Int,
        toneMode: ToneMode
    ): String {
        val langAttr = mapLanguageToBCP47(language)
        // Language-specific tuning takes precedence: ta→slow+600, hi→medium+500, kn→slow+600, en→medium+400
        val (langRate, breakMs) = resolveLanguageTuning(language)?.let { (r, b) -> r to b }
            ?: run {
                val baseRate = when {
                    age in 3..5 -> "x-slow"
                    age in 6..8 -> "slow"
                    else -> "medium"
                }
                val b = when { age in 3..5 -> 300; age in 6..8 -> 250; else -> 200 }
                val r = when (language.lowercase()) {
                    "ta" -> when (baseRate) { "medium" -> "slow"; "slow" -> "x-slow"; else -> baseRate }
                    "hi" -> baseRate
                    else -> when (baseRate) { "x-slow" -> "slow"; "slow" -> "medium"; else -> baseRate }
                }
                r to b
            }
        val outerPitch = if (language.lowercase() == "ta") " pitch=\"-5%\"" else ""

        val innerParts = emotionTagged.segments.mapIndexed { i, seg ->
            val prosodyAttrs = emotionToProsody(seg.emotion)
            val escaped = escapeXml(seg.text)
            val wrapped = if (prosodyAttrs.isBlank()) escaped else "<prosody $prosodyAttrs>$escaped</prosody>"
            if (i < emotionTagged.segments.size - 1) {
                // Minimal break between segments for smooth flow; slight pause for conversational
                val segmentBreakMs = if (seg.emotion == EmotionTag.CONVERSATIONAL) {
                    (breakMs + 80).coerceAtMost(400)
                } else breakMs
                "$wrapped<break time=\"${segmentBreakMs}ms\"/>"
            } else wrapped
        }
        val innerContent = innerParts.joinToString("\n")

        val ssml = """
            <speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis">
                <lang xml:lang="$langAttr">
                    <prosody rate="$langRate"$outerPitch>
                        $innerContent
                    </prosody>
                </lang>
            </speak>
        """.trimIndent()
        return validateAndSanitizeSsml(ssml)
    }

    /**
     * Map emotion tag to SSML prosody overrides (pitch, volume, rate).
     * Stronger values = more audible difference (Google TTS needs pronounced cues).
     * CONVERSATIONAL/DIALOGUE: warmer pitch for "real conversation" feel.
     */
    private fun emotionToProsody(emotion: EmotionTag): String = when (emotion) {
        EmotionTag.CALM -> ""
        EmotionTag.SOFT_SUSPENSE -> "pitch=\"-5%\" rate=\"90%\""
        EmotionTag.EXCITED -> "pitch=\"+8%\" rate=\"105%\""
        EmotionTag.WHISPER -> "volume=\"soft\" rate=\"85%\""
        EmotionTag.DIALOGUE -> "pitch=\"+6%\""  // Pronounced lift for quoted speech—sounds more like conversation
        EmotionTag.CONVERSATIONAL -> "pitch=\"+4%\" rate=\"102%\""  // Warm, inviting; slight pace increase for questions
    }

    /**
     * Language-specific SSML tuning for natural conversational pacing.
     * Hindi and English: slow rate for relaxed storytelling (kids 2–12).
     */
    private fun resolveLanguageTuning(language: String): Pair<String, Int>? {
        return when (language.trim().lowercase()) {
            "ta" -> "slow" to 300      // Tamil: storytelling pace
            "hi" -> "slow" to 300     // Hindi: slower for clarity, comfortable pauses
            "te" -> "slow" to 280     // Telugu
            "kn" -> "slow" to 300     // Kannada
            "ml" -> "slow" to 280     // Malayalam
            "en" -> "slow" to 300     // English: slower for kid-friendly listening
            else -> null
        }
    }

    /** Language-based rate: Indic languages typically benefit from slightly slower default. */
    private fun resolveRate(language: String, age: Int, toneMode: ToneMode): String {
        resolveLanguageTuning(language)?.let { (rate, _) -> return rate }
        val baseRate = when {
            age < 6 -> "slow"
            toneMode == ToneMode.CALM -> "medium"
            toneMode == ToneMode.EXPRESSIVE -> "medium"
            else -> "medium"
        }
        val langSlower = language.lowercase() in setOf("hi", "ta", "te", "kn", "ml", "bn")
        return when {
            age < 6 -> "slow"
            langSlower && toneMode == ToneMode.CALM -> "x-slow"  // Indic + calm: slower for clarity
            langSlower -> "slow"
            else -> baseRate
        }
    }

    /** Age-based break timing; shorter gaps for smooth narration. */
    private fun resolveBreakTimeMs(language: String, age: Int): Int {
        resolveLanguageTuning(language)?.let { (_, breakMs) -> return breakMs }
        return when {
            age < 4 -> 280
            age < 6 -> 250
            age < 8 -> 220
            else -> 200
        }
    }

    /** Ensure valid SSML: no stray unescaped chars, balanced structure. */
    private fun validateAndSanitizeSsml(ssml: String): String {
        return ssml
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), "")  // Strip invalid XML chars
    }

    private fun mapLanguageToBCP47(lang: String): String {
        val m = mapOf(
            "en" to "en-US",
            "hi" to "hi-IN",
            "ta" to "ta-IN",
            "te" to "te-IN",
            "kn" to "kn-IN",
            "ml" to "ml-IN"
        )
        return m[lang.lowercase()] ?: lang
    }

    private fun escapeXml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
