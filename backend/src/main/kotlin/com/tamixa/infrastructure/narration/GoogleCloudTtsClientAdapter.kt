package com.tamixa.infrastructure.narration

import com.tamixa.application.narration.NarrationTextUtils
import com.tamixa.application.port.narration.TtsClientPort
import com.tamixa.infrastructure.http.ApiErrorExtractor
import com.tamixa.infrastructure.observability.AiApiMetrics
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Google Cloud Text-to-Speech adapter with native Indian language voices.
 * Uses voices trained on native speakers (ta-IN, hi-IN, etc.) for correct pronunciation.
 *
 * Required: NARRATION_TTS_PROVIDER=google, GOOGLE_CLOUD_TTS_API_KEY or GOOGLE_APPLICATION_CREDENTIALS
 */
@Component
@ConditionalOnProperty(name = ["app.narration.tts-provider"], havingValue = "google")
class GoogleCloudTtsClientAdapter(
    @Qualifier("googleTtsRestTemplate") private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @org.springframework.beans.factory.annotation.Autowired(required = false) private val aiApiMetrics: AiApiMetrics?,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.google-tts-api-key:}") private val apiKey: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.google-tts-voice-name:Achernar}") private val voiceNameSuffix: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.google-tts-speed:0.95}") private val speed: Double,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.google-tts-use-wavenet-for-indic:true}") private val useWavenetForIndic: Boolean,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.google-tts-audio-encoding:LINEAR16}") private val audioEncoding: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.google-tts-sample-rate-hertz:44100}") private val sampleRateHertz: Int,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.google-tts-volume-gain-db:0}") private val volumeGainDb: Double,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.google-tts-voice-overrides:}") private val voiceOverridesRaw: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.style-profile:default}") private val styleProfile: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.modulation-intensity:balanced}") private val modulationIntensity: String,
    @org.springframework.beans.factory.annotation.Value("\${app.narration.preset:}") private val narrationPreset: String
) : TtsClientPort {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Chirp3-HD voice names supported by Google for Indian languages (ta-IN, hi-IN, etc.). */
    private val CHIRP3_HD_VOICE_NAMES = setOf(
        "achernar", "achird", "algenib", "algieba", "alnilam", "aoede", "autonoe", "callirrhoe",
        "charon", "despina", "enceladus", "erinome", "fenrir", "gacrux", "iapetus", "kore",
        "laomedeia", "leda", "orus", "pulcherrima", "puck", "rasalgethi", "sadachbia", "sadaltager",
        "schedar", "sulafat", "umbriel", "vindemiatrix", "zephyr", "zubenelgenubi"
    )

    /** Language code to BCP-47 (e.g. ta -> ta-IN). Full names supported so "tamil" -> ta-IN. */
    private val LANGUAGE_CODES = mapOf(
        "ta" to "ta-IN", "tamil" to "ta-IN",
        "hi" to "hi-IN", "hindi" to "hi-IN",
        "te" to "te-IN", "telugu" to "te-IN",
        "kn" to "kn-IN", "kannada" to "kn-IN",
        "ml" to "ml-IN", "malayalam" to "ml-IN",
        "bn" to "bn-IN", "bengali" to "bn-IN",
        "en" to "en-IN", "english" to "en-IN",
        "en-us" to "en-US", "en-gb" to "en-GB"
    )

    private val voiceOverrides: Map<String, String> = parseVoiceOverrides(voiceOverridesRaw)

    private val roleSpecificChirpVoice = mapOf(
        "narrator-female" to "Achernar",
        "child-female" to "Kore",
        "child-male" to "Charon",
        "child-group" to "Kore",
        "default" to "Achernar"
    )

    /** Chirp3-HD voices: most natural, native-sounding for Indian languages. */
    private fun nativeVoiceName(languageCode: String, preferredSuffix: String? = null): String {
        val requestedSuffix = preferredSuffix?.trim().orEmpty().ifBlank { voiceNameSuffix.trim() }
        val normalized = if (requestedSuffix.isNotBlank() && requestedSuffix.lowercase() in CHIRP3_HD_VOICE_NAMES) {
            requestedSuffix.lowercase().replaceFirstChar { it.uppercaseChar() }
        } else {
            "Achernar"
        }
        return "$languageCode-Chirp3-HD-$normalized"
    }

    /** Google TTS input limit: 5000 bytes (UTF-8). Use 4500 for safety. */
    private val MAX_INPUT_BYTES = 4500

    override fun synthesizeToMp3(ssml: String, language: String, voiceProfile: String): ByteArray? {
        if (apiKey.isBlank()) {
            log.warn("Google Cloud TTS skipped: GOOGLE_CLOUD_TTS_API_KEY is blank")
            return null
        }
        val trimmed = ssml.trim()
        val plainText = ssmlToPlainText(trimmed)
        if (plainText.isBlank()) {
            log.warn("Google Cloud TTS: SSML yielded empty text")
            return null
        }
        val (languageCode, voiceCandidates) = resolveVoiceCandidates(language, voiceProfile)
        aiApiMetrics?.recordGoogleTts(1, plainText.length)
        log.info(
            "Google TTS: requested language={} -> languageCode={} voices={}",
            language,
            languageCode,
            voiceCandidates.joinToString("|")
        )
        val effectiveSpeed = applyStyleToSpeed(speed.coerceIn(0.25, 2.0))

        var lastFailure: Exception? = null
        for ((idx, voiceName) in voiceCandidates.withIndex()) {
            try {
                val audio = synthesizeWithVoice(trimmed, plainText, language, languageCode, voiceName, effectiveSpeed)
                if (idx > 0) {
                    log.info(
                        "Google TTS voice fallback recovered synthesis: lang={} languageCode={} voice={}",
                        language,
                        languageCode,
                        voiceName
                    )
                }
                return audio
            } catch (e: Exception) {
                lastFailure = e
                val msg = ApiErrorExtractor.extract(e, objectMapper)
                log.warn(
                    "Google TTS voice candidate failed ({}/{}): languageCode={} voice={} err={}",
                    idx + 1,
                    voiceCandidates.size,
                    languageCode,
                    voiceName,
                    msg
                )
            }
        }

        throw IllegalStateException(
            "Google TTS failed for all candidate voices: ${lastFailure?.message ?: "unknown error"}",
            lastFailure
        )
    }

    private fun synthesizeWithVoice(
        trimmedSsml: String,
        plainText: String,
        language: String,
        languageCode: String,
        voiceName: String,
        effectiveSpeed: Double
    ): ByteArray {
        // Prefer SSML when valid (has prosody/breaks for expression) and fits or can be chunked
        val ssmlChunks = if (isValidSsml(trimmedSsml)) chunkSsml(trimmedSsml) else emptyList()
        val useSsml = ssmlChunks.isNotEmpty()
        val allBytes = mutableListOf<ByteArray>()
        var ssmlFailed = false

        if (useSsml) {
            for ((i, ssmlChunk) in ssmlChunks.withIndex()) {
                try {
                    val bytes = synthesizeChunkWithSsml(ssmlChunk, languageCode, voiceName, effectiveSpeed)
                    if (bytes != null && bytes.isNotEmpty()) {
                        allBytes.add(bytes)
                    } else {
                        log.warn("Google TTS SSML returned empty audio for chunk {}/{}", i + 1, ssmlChunks.size)
                    }
                } catch (e: Exception) {
                    val msg = ApiErrorExtractor.extract(e, objectMapper)
                    log.warn("Google TTS SSML failed for chunk {}/{}: {}", i + 1, ssmlChunks.size, msg, e)
                    ssmlFailed = true
                    break
                }
            }
        }

        if (allBytes.isEmpty() || ssmlFailed) {
            if (ssmlFailed) {
                allBytes.clear()
                log.warn("Google TTS: falling back to plain text chunking after SSML failure")
            }
            // Fallback: plain text (loses prosody but works for any size)
            val textChunks = chunkByByteLimit(plainText, MAX_INPUT_BYTES)
            for ((i, chunk) in textChunks.withIndex()) {
                try {
                    val bytes = synthesizeChunk(chunk, languageCode, voiceName, effectiveSpeed)
                    if (bytes != null && bytes.isNotEmpty()) {
                        allBytes.add(bytes)
                    } else if (chunk.isNotBlank()) {
                        log.warn("Google TTS returned empty audio for chunk {}/{}", i + 1, textChunks.size)
                    }
                } catch (e: Exception) {
                    val msg = ApiErrorExtractor.extract(e, objectMapper)
                    log.warn("Google TTS failed for chunk {}/{}: {}", i + 1, textChunks.size, msg, e)
                    throw RuntimeException("Google TTS: $msg", e)
                }
            }
        }

        if (allBytes.isEmpty()) {
            log.warn("Google TTS produced no audio")
            throw RuntimeException("Google TTS: produced no audio")
        }
        val combined = allBytes.reduce { a, b -> a + b }
        log.debug("Google TTS success: {} chunks, {} bytes total for lang={} (ssml={})", allBytes.size, combined.size, language, useSsml)
        return combined
    }

    /** True if SSML looks structured (speak/prosody/break) – safe to pass to API. */
    private fun isValidSsml(ssml: String): Boolean =
        ssml.contains("<speak", ignoreCase = true) && ssml.contains("</speak>", ignoreCase = true)

    /**
     * Chunk SSML for Google's 5000-byte limit. Splits at </prosody> or </p> boundaries
     * and wraps each chunk in minimal speak/lang/prosody shell.
     */
    private fun chunkSsml(ssml: String): List<String> {
        val bytes = ssml.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size <= MAX_INPUT_BYTES) return listOf(ssml)

        // Extract outer lang and prosody attributes to reapply to chunks
        val langMatch = Regex("""xml:lang="([^"]+)"""").find(ssml)
        val langAttr = langMatch?.groupValues?.get(1) ?: "en-US"
        val outerProsodyMatch = Regex("""<prosody\s+([^>]+)>""").find(ssml)
        val outerAttrs = outerProsodyMatch?.groupValues?.get(1) ?: "rate=\"medium\""

        // Extract inner content between first <prosody...> and last </prosody>
        val prosodyOpen = ssml.indexOf("<prosody")
        val innerStart = if (prosodyOpen >= 0) ssml.indexOf(">", prosodyOpen) + 1 else -1
        val innerEnd = ssml.lastIndexOf("</prosody>")
        val inner = if (innerStart > 0 && innerEnd > innerStart) ssml.substring(innerStart, innerEnd) else null
        if (inner == null) return emptyList()  // Can't parse; fall back to plain-text path

        // Split at segment boundaries: after </prosody> or </p> followed by optional break
        val segmentRegex = Regex("""(</prosody>|</p>)\s*(?:<break[^>]*/>)?""")
        val segmentEnds = segmentRegex.findAll(inner).map { it.range.last + 1 }.toList()
        val boundaries = if (segmentEnds.isEmpty()) listOf(inner.length) else segmentEnds + inner.length

        val chunks = mutableListOf<String>()
        var start = 0
        var currentChunk = StringBuilder()

        for (i in boundaries.indices) {
            val end = boundaries[i]
            val segment = inner.substring(start, end.coerceAtMost(inner.length)).trim()
            if (segment.isBlank()) {
                start = end
                continue
            }
            val candidate = if (currentChunk.isEmpty()) segment else currentChunk.toString() + segment
            if (candidate.toByteArray(StandardCharsets.UTF_8).size + 200 <= MAX_INPUT_BYTES) {
                currentChunk.append(segment)
            } else {
                if (currentChunk.isNotEmpty()) {
                    val wrapped = wrapSsmlChunk(currentChunk.toString(), langAttr, outerAttrs)
                    chunks.add(wrapped)
                    currentChunk = StringBuilder()
                }
                if (segment.toByteArray(StandardCharsets.UTF_8).size + 200 <= MAX_INPUT_BYTES) {
                    currentChunk.append(segment)
                } else {
                    chunks.add(wrapSsmlChunk(segment, langAttr, outerAttrs))
                }
            }
            start = end
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(wrapSsmlChunk(currentChunk.toString(), langAttr, outerAttrs))
        }
        // If any SSML chunk still exceeds the byte limit (e.g. one giant segment), switch to plain-text chunking fallback.
        if (chunks.any { it.toByteArray(StandardCharsets.UTF_8).size > MAX_INPUT_BYTES }) {
            log.warn("Google TTS SSML chunk still exceeds {} bytes; falling back to plain-text chunking", MAX_INPUT_BYTES)
            return emptyList()
        }
        return chunks
    }

    private fun wrapSsmlChunk(inner: String, langAttr: String, outerProsodyAttrs: String): String =
        """<speak version="1.0" xmlns="http://www.w3.org/2001/10/synthesis"><lang xml:lang="$langAttr"><prosody $outerProsodyAttrs>$inner</prosody></lang></speak>"""

    private fun buildAudioConfig(speakingRate: Double): Map<String, Any> {
        val config = mutableMapOf<String, Any>(
            "audioEncoding" to (audioEncoding.takeIf { it.equals("LINEAR16", ignoreCase = true) } ?: "MP3"),
            "speakingRate" to speakingRate.coerceIn(0.25, 2.0),
            "volumeGainDb" to volumeGainDb.coerceIn(-96.0, 16.0)
        )
        if (audioEncoding.equals("LINEAR16", ignoreCase = true) && sampleRateHertz > 0) {
            config["sampleRateHertz"] = sampleRateHertz
        }
        return config
    }

    private fun synthesizeChunkWithSsml(ssmlChunk: String, languageCode: String, voiceName: String, speakingRate: Double): ByteArray? {
        val request = mapOf(
            "input" to mapOf("ssml" to ssmlChunk),
            "voice" to mapOf(
                "languageCode" to languageCode,
                "name" to voiceName
            ),
            "audioConfig" to buildAudioConfig(speakingRate)
        )
        val headers = HttpHeaders().apply {
            set("Content-Type", "application/json")
            set("X-Goog-Api-Key", apiKey)
        }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)
        val url = "https://texttospeech.googleapis.com/v1/text:synthesize"
        val response = restTemplate.exchange(url, HttpMethod.POST, entity, String::class.java)
        val body = response.body ?: return null
        return extractAudioBytesFromResponse(body)
    }

    /** Split text into chunks under maxBytes (UTF-8), preferring sentence boundaries. */
    private fun chunkByByteLimit(text: String, maxBytes: Int): List<String> {
        val result = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty()) {
            if (remaining.toByteArray(StandardCharsets.UTF_8).size <= maxBytes) {
                result.add(remaining)
                break
            }
            var splitAt = -1
            var byteCount = 0
            val chars = remaining.toCharArray()
            val sentenceEnders = charArrayOf('.', '!', '?', '\n', '।', '॥')  // । ॥ = Devanagari danda
            for (i in chars.indices) {
                byteCount += Character.toString(chars[i]).toByteArray(StandardCharsets.UTF_8).size
                if (byteCount > maxBytes) {
                    // Find best break before this point (sentence end > space > char)
                    for (j in i downTo 0) {
                        when {
                            chars[j] in sentenceEnders -> { splitAt = j + 1; break }
                            chars[j] == ' ' -> { splitAt = j; break }
                        }
                    }
                    if (splitAt <= 0) splitAt = i.coerceAtLeast(1)
                    break
                }
                if (chars[i] in sentenceEnders) splitAt = i + 1
                else if (chars[i] == ' ') splitAt = i
            }
            if (splitAt <= 0) splitAt = remaining.length
            result.add(remaining.substring(0, splitAt).trim())
            remaining = remaining.substring(splitAt).trim()
        }
        return result
    }

    private fun synthesizeChunk(text: String, languageCode: String, voiceName: String, effectiveSpeed: Double): ByteArray? {
        val request = mapOf(
            "input" to mapOf("text" to text),
            "voice" to mapOf(
                "languageCode" to languageCode,
                "name" to voiceName
            ),
            "audioConfig" to buildAudioConfig(effectiveSpeed)
        )
        val headers = HttpHeaders().apply {
            set("Content-Type", "application/json")
            set("X-Goog-Api-Key", apiKey)
        }
        val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)
        val url = "https://texttospeech.googleapis.com/v1/text:synthesize"
        val response = restTemplate.exchange(url, HttpMethod.POST, entity, String::class.java)
        val body = response.body ?: return null
        return extractAudioBytesFromResponse(body)
    }

    private fun extractAudioBytesFromResponse(body: String): ByteArray? {
        val match = Regex("\"audioContent\"\\s*:\\s*\"([^\"]+)\"").find(body)
        if (match != null) {
            return Base64.getDecoder().decode(match.groupValues[1])
        }
        val compact = body.replace('\n', ' ').replace('\r', ' ').take(300)
        throw IllegalStateException("Google TTS response missing audioContent: $compact")
    }

    private fun ssmlToPlainText(ssml: String): String {
        val plain = ssml
            .replace(Regex("<break[^>]*/>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
        val cleaned = NarrationTextUtils.stripRemainingMarkers(plain)
        return cleaned.ifBlank { plain }.ifBlank { ssml }
    }

    /** Returns (languageCode, candidate voices) with role-aware and per-language fallback selection. */
    private fun resolveVoiceCandidates(language: String, voiceProfile: String): Pair<String, List<String>> {
        val langCode = normalizeLanguageCode(language)
        val profile = normalizeVoiceProfile(voiceProfile)
        val candidates = linkedSetOf<String>()

        val override = voiceOverrides[langCode]
        if (!override.isNullOrBlank()) {
            candidates.add(override)
        }

        val wavenetVoice = defaultWaveNetVoice(langCode, profile)
        if (useWavenetForIndic && wavenetVoice != null) {
            candidates.add(wavenetVoice)
        }

        val chirpSuffix = roleSpecificChirpVoice[profile] ?: roleSpecificChirpVoice.getValue("default")
        candidates.add(nativeVoiceName(langCode, chirpSuffix))
        // Provider-safe final fallback; en-IN usually sounds natural for Indian audiences.
        candidates.add(nativeVoiceName("en-IN", "Achernar"))
        return langCode to candidates.toList()
    }

    private fun normalizeLanguageCode(language: String): String {
        val normalized = language.trim().lowercase()
        if (normalized.matches(Regex("^[a-z]{2}-[a-z]{2}$"))) {
            val parts = normalized.split("-")
            return "${parts[0]}-${parts[1].uppercase()}"
        }
        return LANGUAGE_CODES[normalized] ?: run {
            log.warn("Google TTS: unknown language \"{}\", falling back to en-IN", language)
            "en-IN"
        }
    }

    private fun normalizeVoiceProfile(voiceProfile: String): String {
        val raw = voiceProfile.trim().lowercase()
        if (raw.isBlank()) return "default"
        return when {
            raw.contains("narrator") && raw.contains("female") -> "narrator-female"
            raw.contains("child") && raw.contains("female") -> "child-female"
            raw.contains("child") && raw.contains("male") -> "child-male"
            raw.contains("group") -> "child-group"
            else -> "default"
        }
    }

    /**
     * WaveNet gives very natural regional pronunciation for supported Indic languages.
     * For unsupported role/language combinations we intentionally fall back to Chirp3-HD.
     */
    private fun defaultWaveNetVoice(langCode: String, voiceProfile: String): String? {
        return when (langCode) {
            "ta-IN" -> if (voiceProfile == "child-male") "ta-IN-Wavenet-B" else "ta-IN-Wavenet-A"
            "hi-IN" -> if (voiceProfile == "child-male") "hi-IN-Wavenet-B" else "hi-IN-Wavenet-A"
            "te-IN" -> if (voiceProfile == "child-male") "te-IN-Standard-B" else "te-IN-Standard-A"
            "kn-IN" -> if (voiceProfile == "child-male") "kn-IN-Wavenet-B" else "kn-IN-Wavenet-A"
            "ml-IN" -> if (voiceProfile == "child-male") "ml-IN-Wavenet-B" else "ml-IN-Wavenet-A"
            "bn-IN" -> if (voiceProfile == "child-male") "bn-IN-Wavenet-B" else "bn-IN-Wavenet-A"
            else -> null
        }
    }

    private fun parseVoiceOverrides(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",")
            .mapNotNull { entry ->
                val parts = entry.split("=", limit = 2).map { it.trim() }
                if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) null
                else normalizeLanguageCode(parts[0]) to parts[1]
            }
            .toMap()
    }

    private fun applyStyleToSpeed(baseSpeed: Double): Double {
        val style = resolveEffectiveStyleProfile()
        val styleAdjusted = when (style) {
            "kid-energy" -> baseSpeed * 1.06
            "bedtime-calm" -> baseSpeed * 0.93
            else -> baseSpeed
        }
        val intensityFactor = when (resolveEffectiveModulationIntensity()) {
            "soft" -> 0.97
            "high" -> 1.03
            else -> 1.0
        }
        return (styleAdjusted * intensityFactor).coerceIn(0.25, 2.0)
    }

    private fun resolveEffectiveStyleProfile(): String {
        return when (narrationPreset.trim().lowercase()) {
            "bedtime" -> "bedtime-calm"
            "energetic" -> "kid-energy"
            "balanced" -> "default"
            else -> styleProfile.trim().lowercase()
        }
    }

    private fun resolveEffectiveModulationIntensity(): String {
        return when (narrationPreset.trim().lowercase()) {
            "bedtime" -> "soft"
            "energetic" -> "high"
            "balanced" -> "balanced"
            else -> modulationIntensity.trim().lowercase()
        }
    }
}
