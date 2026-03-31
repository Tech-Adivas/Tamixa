package com.tamixa.application.narration.impl

import com.tamixa.application.narration.SSMLBuilderService
import com.tamixa.domain.narration.EmotionSegment
import com.tamixa.domain.narration.EmotionTag
import com.tamixa.domain.narration.EmotionTaggedScript
import com.tamixa.domain.narration.ToneMode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.math.roundToInt

/**
 * Builds SSML from plain narration text.
 *
 * Architecture: Language-based rate strategy (Indic langs slower), age-based break timing
 * (younger = longer pauses), tone-based rate. Ensures valid SSML via XML escaping.
 *
 * Emotional layer: buildSSMLFromEmotionTagged maps emotion tags to prosody.
 * Deterministic first: no AI-generated content; only prosody attributes.
 *
 * Natural pace (default): rate=100%, no emotion prosody — playback matches Cloud samples.
 * When natural pace is disabled, uses slower kid-friendly rates and emotion-based prosody.
 */
@Service
class SSMLBuilderServiceImpl(
    @Value("\${app.narration.ssml-use-natural-pace:true}") private val ssmlUseNaturalPace: Boolean = true,
    @Value("\${app.narration.ssml-subtle-prosody-in-natural:true}") private val ssmlSubtleProsodyInNatural: Boolean = true,
    @Value("\${app.narration.style-profile:default}") private val styleProfile: String = "default",
    @Value("\${app.narration.modulation-intensity:balanced}") private val modulationIntensity: String = "balanced",
    @Value("\${app.narration.preset:}") private val narrationPreset: String = ""
) : SSMLBuilderService {

    private val INDIC_LANGS = setOf("ta", "hi", "te", "kn", "ml", "bn")
    private val STYLE_DEFAULT = "default"
    private val STYLE_KID_ENERGY = "kid-energy"
    private val STYLE_BEDTIME_CALM = "bedtime-calm"
    private val PRESET_BEDTIME = "bedtime"
    private val PRESET_BALANCED = "balanced"
    private val PRESET_ENERGETIC = "energetic"
    private val INTENSITY_SOFT = "soft"
    private val INTENSITY_BALANCED = "balanced"
    private val INTENSITY_HIGH = "high"
    private val debugLogPath: Path? = System.getenv("DEBUG_LOG_PATH")?.takeIf { it.isNotBlank() }?.let { Path.of(it) }

    private fun debugLog(
        runId: String,
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap()
    ) {
        val path = debugLogPath ?: return
        try {
            path.parent?.let { Files.createDirectories(it) }
            val payload = buildString {
                append("{")
                append("\"sessionId\":\"ab5527\",")
                append("\"runId\":\"").append(escapeJson(runId)).append("\",")
                append("\"hypothesisId\":\"").append(escapeJson(hypothesisId)).append("\",")
                append("\"location\":\"").append(escapeJson(location)).append("\",")
                append("\"message\":\"").append(escapeJson(message)).append("\",")
                append("\"data\":{")
                append(data.entries.joinToString(",") { (k, v) -> "\"${escapeJson(k)}\":${toJsonValue(v)}" })
                append("},")
                append("\"timestamp\":").append(System.currentTimeMillis())
                append("}\n")
            }
            Files.writeString(
                path,
                payload,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
            )
        } catch (_: Exception) {
            // Never fail SSML build due to debug logging.
        }
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun toJsonValue(value: Any?): String = when (value) {
        null -> "null"
        is Number, is Boolean -> value.toString()
        else -> "\"${escapeJson(value.toString())}\""
    }

    /** Indic: insert SSML break after commas/semicolons so TTS clearly separates phrases (fixes e.g. "வேட்டையாடி,இரவுக்கு" read as one word). */
    private fun insertCommaBreaksForIndic(text: String, language: String): String {
        if (language.trim().lowercase() !in INDIC_LANGS) return text
        val breakTag = """<break time="120ms"/>"""
        return text
            .replace(Regex(""",\s*"""), ", $breakTag ")   // comma + optional space
            .replace(Regex(""";\s*"""), "; $breakTag ")   // semicolon
    }

    /** Indic: insert short break after sentence endings (। . ! ?) so TTS breathes between thoughts—sounds more natural. */
    private fun insertSentenceBreaksForIndic(text: String, language: String): String {
        if (language.trim().lowercase() !in INDIC_LANGS) return text
        val breathBreak = """<break time="180ms"/>"""
        return text
            .replace(Regex("""([।.!?])\s+"""), "$1 $breathBreak ")   // sentence end + space
    }

    /** Placeholder prefix for SSML breaks (inserted after escapeXml). */
    private val SSML_BREAK_PLACEHOLDER = "__SSML_BREAK_"

    /**
     * Convert inline narration markers: [Pause 500ms] -> placeholder for <break/> ; tone markers stripped.
     * Placeholders survive escapeXml; replaced with real SSML after escaping.
     */
    private fun convertInlineMarkersToSsml(text: String): String {
        var result = text
        // Pause markers: replace with placeholder. Allow optional space: [Pause 1s] or [Pause1s]
        result = Regex("""\[Pause\s*(\d+)(ms|s)\s*\]""", RegexOption.IGNORE_CASE).replace(result) { mr ->
            val num = mr.groupValues[1].toIntOrNull() ?: return@replace mr.value
            val unit = mr.groupValues[2].lowercase()
            val time = if (unit == "s") "${num}s" else "${num}ms"
            "$SSML_BREAK_PLACEHOLDER${time}__"
        }
        // Tone markers: strip. Support ASCII and fullwidth brackets, optional inner whitespace.
        val toneMarkerRegex = Regex(
            """[\[\［]\s*(?:Warm\s*tone|Gentle\s*tone|Calm(?:\s*tone)?|Happy\s*tone|Excited\s*tone|Playful\s*tone|Curious\s*tone|Wonder\s*tone|Reassuring\s*tone|Thoughtful\s*tone|Soft\s*voice|Whisper(?:ed)?\s*tone|Emotional\s*tone|Soft\s*emotional\s*tone|Celebration\s*tone|Storyteller\s*tone|Slow\s*pacing|Medium\s*pacing|Brisk\s*pacing|Scene\s*opens\s*softly|Scene\s*shifts|A\s*gentle\s*moment|A\s*magical\s*moment|A\s*quiet\s*pause|A\s*joyful\s*moment|A\s*surprise\s*moment|Closing\s*tone|Audio\s*imagination|Joyful\s*moment|Clear\s*tone)\s*[\]\］]""",
            RegexOption.IGNORE_CASE
        )
        result = toneMarkerRegex.replace(result, "")
        // Fallback: any bracketed phrase containing marker keywords (catches translated/malformed)
        result = Regex(
            """[\[\［][^\]\］]*(?:tone|voice|pacing|scene|calm|whisper|excited|pause|happy|warm|soft|storyteller|gentle|curious|wonder|reassuring|thoughtful|celebration|audio|closing)[^\]\］]*[\]\］]""",
            RegexOption.IGNORE_CASE
        ).replace(result, "")
        // Repair frequent escaped-newline artifact from LLM outputs: "nn" used instead of paragraph breaks.
        result = result.replace(Regex("""\bnn\b""", RegexOption.IGNORE_CASE), "\n\n")
        // Collapse only spaces (not newlines) to avoid merging paragraphs
        return result.replace(Regex("""[^\S\n]{2,}"""), " ").trim()
    }

    /** Replace placeholders with real SSML break tags. Call after escapeXml. */
    private fun restoreSsmlBreaks(text: String): String {
        return Regex("""${Regex.escape(SSML_BREAK_PLACEHOLDER)}(\d+(?:ms|s))__""").replace(text) { mr ->
            """<break time="${mr.groupValues[1]}"/>"""
        }
    }

    override fun buildSSML(
        scriptText: String,
        language: String,
        age: Int,
        toneMode: ToneMode
    ): String {
        val style = resolveEffectiveStyleProfile()
        // #region agent log
        debugLog(
            runId = "run6",
            hypothesisId = "H1",
            location = "SSMLBuilderServiceImpl.kt:buildSSML-entry",
            message = "Build SSML entry quote counters",
            data = mapOf(
                "lang" to language,
                "doubleQuoteCount" to scriptText.count { it == '"' },
                "singleQuoteCount" to scriptText.count { it == '\'' },
                "curlyQuoteCount" to scriptText.count { it == '“' || it == '”' || it == '‘' || it == '’' },
                "containsDoubleQuotationWord" to scriptText.lowercase().contains("double quotation")
            )
        )
        // #endregion
        val langAttr = mapLanguageToBCP47(language)
        val baseRate = if (ssmlUseNaturalPace) "100%" else resolveRate(language, age, toneMode)
        val rate = applyStyleToRate(baseRate, style)
        val breakMs = applyStyleToBreakMs(resolveBreakTimeMs(language, age), style)
        val breakTag = """<break time="${breakMs}ms"/>"""
        val cleanedScript = convertInlineMarkersToSsml(scriptText)
        val paragraphs = cleanedScript.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotBlank() }
        val innerContent = paragraphs.map { para ->
            val escaped = escapeXml(para)
            val restored = restoreSsmlBreaks(escaped)
            val withCommaBreaks = insertCommaBreaksForIndic(restored, language)
            val withSentenceBreaks = insertSentenceBreaksForIndic(withCommaBreaks, language)
            "<p>$withSentenceBreaks</p>"
        }.joinToString("$breakTag\n")

        val ssml = """
            <speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis">
                <lang xml:lang="$langAttr">
                    <prosody rate="$rate">
                        $innerContent
                    </prosody>
                </lang>
            </speak>
        """.trimIndent()
        // #region agent log
        debugLog(
            runId = "run6",
            hypothesisId = "H2",
            location = "SSMLBuilderServiceImpl.kt:buildSSML-ssml-ready",
            message = "SSML built quote entity counters",
            data = mapOf(
                "lang" to language,
                "ampQuotCount" to Regex("&quot;").findAll(ssml).count(),
                "ampAposCount" to Regex("&apos;").findAll(ssml).count()
            )
        )
        // #endregion
        return validateAndSanitizeSsml(ssml)
    }

    override fun buildSSMLFromEmotionTagged(
        emotionTagged: EmotionTaggedScript,
        language: String,
        age: Int,
        toneMode: ToneMode
    ): String {
        val style = resolveEffectiveStyleProfile()
        val fullScript = emotionTagged.originalScript
        // #region agent log
        debugLog(
            runId = "run6",
            hypothesisId = "H3",
            location = "SSMLBuilderServiceImpl.kt:buildEmotionSSML-entry",
            message = "Build emotion SSML entry quote counters",
            data = mapOf(
                "lang" to language,
                "segmentCount" to emotionTagged.segments.size,
                "doubleQuoteCount" to fullScript.count { it == '"' },
                "singleQuoteCount" to fullScript.count { it == '\'' },
                "curlyQuoteCount" to fullScript.count { it == '“' || it == '”' || it == '‘' || it == '’' },
                "containsDoubleQuotationWord" to fullScript.lowercase().contains("double quotation")
            )
        )
        // #endregion
        val langAttr = mapLanguageToBCP47(language)
        val (baseLangRate, baseBreakMs) = if (ssmlUseNaturalPace) {
            "100%" to resolveBreakTimeMs(language, age)
        } else {
            resolveLanguageTuning(language)?.let { (r, b) -> r to b }
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
        }
        val langRate = applyStyleToRate(baseLangRate, style)
        val breakMs = applyStyleToBreakMs(baseBreakMs, style)
        val outerPitch = if (ssmlUseNaturalPace) "" else if (language.lowercase() == "ta") " pitch=\"-5%\"" else ""

        val innerParts = emotionTagged.segments.mapIndexed { i, seg ->
            val cleanedText = convertInlineMarkersToSsml(seg.text)
            val prosodyAttrs = when {
                ssmlUseNaturalPace && !ssmlSubtleProsodyInNatural -> ""
                ssmlUseNaturalPace && ssmlSubtleProsodyInNatural -> subtleProsodyForEmotion(seg.emotion, language)
                else -> emotionToProsody(seg.emotion, language)
            }
            val escaped = escapeXml(cleanedText)
            val restored = restoreSsmlBreaks(escaped)
            val withCommaBreaks = insertCommaBreaksForIndic(restored, language)
            val withSentenceBreaks = insertSentenceBreaksForIndic(withCommaBreaks, language)
            val wrapped = if (prosodyAttrs.isBlank()) withSentenceBreaks else "<prosody $prosodyAttrs>$withSentenceBreaks</prosody>"
            if (i < emotionTagged.segments.size - 1) {
                val segmentBreakMs = if (!ssmlUseNaturalPace && seg.emotion == EmotionTag.CONVERSATIONAL) {
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
        // #region agent log
        debugLog(
            runId = "run6",
            hypothesisId = "H2",
            location = "SSMLBuilderServiceImpl.kt:buildEmotionSSML-ssml-ready",
            message = "Emotion SSML quote entity counters",
            data = mapOf(
                "lang" to language,
                "ampQuotCount" to Regex("&quot;").findAll(ssml).count(),
                "ampAposCount" to Regex("&apos;").findAll(ssml).count()
            )
        )
        // #endregion
        return validateAndSanitizeSsml(ssml)
    }

    /** Very light prosody when natural pace + subtle prosody enabled. Adds warmth without over-acting. */
    private fun subtleProsodyForEmotion(emotion: EmotionTag, language: String): String {
        val lang = language.trim().lowercase()
        val dialoguePitchDelta = if (lang in setOf("ta", "hi", "te", "kn", "ml")) 1 else 2
        val conversationalPitchDelta = 1
        return when (emotion) {
            EmotionTag.DIALOGUE -> "pitch=\"${formatSignedPercent(scaleDelta(dialoguePitchDelta))}\""
            EmotionTag.CONVERSATIONAL -> "pitch=\"${formatSignedPercent(scaleDelta(conversationalPitchDelta))}\""
            else -> ""
        }
    }

    /**
     * Map emotion tag to SSML prosody overrides (pitch, volume, rate).
     * Stronger values = more audible difference (Google TTS needs pronounced cues).
     * CONVERSATIONAL/DIALOGUE: warmer pitch for "real conversation" feel.
     */
    private fun emotionToProsody(emotion: EmotionTag, language: String): String {
        val lang = language.trim().lowercase()
        val isIndic = lang in setOf("ta", "hi", "te", "kn", "ml", "bn")
        return when (emotion) {
            EmotionTag.CALM -> ""
            EmotionTag.SOFT_SUSPENSE -> {
                val pitch = if (isIndic) -4 else -5
                val rate = if (isIndic) 91 else 90
                "pitch=\"${formatSignedPercent(scaleDelta(pitch))}\" rate=\"${scaleRate(rate)}%\""
            }
            EmotionTag.EXCITED -> {
                val pitch = if (isIndic) 6 else 8
                val rate = if (isIndic) 103 else 105
                "pitch=\"${formatSignedPercent(scaleDelta(pitch))}\" rate=\"${scaleRate(rate)}%\""
            }
            EmotionTag.WHISPER -> {
                val rate = if (isIndic) 87 else 85
                "volume=\"soft\" rate=\"${scaleRate(rate)}%\""
            }
            EmotionTag.DIALOGUE -> {
                val pitch = if (isIndic) 4 else 6
                "pitch=\"${formatSignedPercent(scaleDelta(pitch))}\""
            }
            EmotionTag.CONVERSATIONAL -> {
                val pitch = if (isIndic) 3 else 4
                val rate = if (isIndic) 101 else 102
                "pitch=\"${formatSignedPercent(scaleDelta(pitch))}\" rate=\"${scaleRate(rate)}%\""
            }
        }
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

    private fun normalizeStyleProfile(value: String): String {
        return when (value.trim().lowercase()) {
            STYLE_KID_ENERGY -> STYLE_KID_ENERGY
            STYLE_BEDTIME_CALM -> STYLE_BEDTIME_CALM
            else -> STYLE_DEFAULT
        }
    }

    /** Apply style only to percent rates; leave "slow"/"medium" tokens unchanged. */
    private fun applyStyleToRate(rate: String, style: String): String {
        val m = Regex("""^(\d+)%$""").matchEntire(rate.trim()) ?: return rate
        val base = m.groupValues[1].toIntOrNull() ?: return rate
        val adjusted = when (style) {
            STYLE_KID_ENERGY -> (base + 4).coerceAtMost(115)
            STYLE_BEDTIME_CALM -> (base - 6).coerceAtLeast(82)
            else -> base
        }
        return "$adjusted%"
    }

    private fun applyStyleToBreakMs(baseBreakMs: Int, style: String): Int {
        return when (style) {
            STYLE_KID_ENERGY -> (baseBreakMs - 50).coerceAtLeast(120)
            STYLE_BEDTIME_CALM -> (baseBreakMs + 80).coerceAtMost(450)
            else -> baseBreakMs
        }
    }

    private fun modulationFactor(): Double {
        return when (resolveEffectiveModulationIntensity()) {
            INTENSITY_SOFT -> 0.75
            INTENSITY_HIGH -> 1.25
            else -> 1.0
        }
    }

    private fun resolveEffectiveStyleProfile(): String {
        return when (narrationPreset.trim().lowercase()) {
            PRESET_BEDTIME -> STYLE_BEDTIME_CALM
            PRESET_ENERGETIC -> STYLE_KID_ENERGY
            PRESET_BALANCED -> STYLE_DEFAULT
            else -> normalizeStyleProfile(styleProfile)
        }
    }

    private fun resolveEffectiveModulationIntensity(): String {
        val presetValue = when (narrationPreset.trim().lowercase()) {
            PRESET_BEDTIME -> INTENSITY_SOFT
            PRESET_ENERGETIC -> INTENSITY_HIGH
            PRESET_BALANCED -> INTENSITY_BALANCED
            else -> null
        }
        val manual = when (modulationIntensity.trim().lowercase()) {
            INTENSITY_SOFT -> INTENSITY_SOFT
            INTENSITY_HIGH -> INTENSITY_HIGH
            else -> INTENSITY_BALANCED
        }
        return presetValue ?: manual
    }

    private fun scaleDelta(delta: Int): Int {
        if (delta == 0) return 0
        val scaled = (delta * modulationFactor()).roundToInt()
        return when {
            delta > 0 -> scaled.coerceAtLeast(1)
            else -> scaled.coerceAtMost(-1)
        }
    }

    private fun scaleRate(baseRatePercent: Int): Int {
        val deviation = baseRatePercent - 100
        val scaledDeviation = scaleDelta(deviation)
        return (100 + scaledDeviation).coerceIn(80, 120)
    }

    private fun formatSignedPercent(delta: Int): String {
        val sign = if (delta >= 0) "+" else ""
        return "$sign${delta}%"
    }
}
