package com.tamixa.infrastructure.voice

import com.tamixa.application.narration.NarrationTextUtils
import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
import com.tamixa.application.port.voice.ElevenLabsVoiceSettings
import com.tamixa.infrastructure.narration.Mp3Joiner
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import jakarta.annotation.PostConstruct
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * ElevenLabs Instant Voice Cloning (IVC) adapter.
 *
 * Implements the [Instant Voice Cloning API](https://elevenlabs.io/docs/eleven-api/guides/cookbooks/voices/instant-voice-cloning):
 * - Create clone: POST /v1/voices/add (multipart: name, files; optional: remove_background_noise, description, labels)
 * - TTS: POST /v1/text-to-speech/{voice_id}. A story is sent in sentence groups of up to [MAX_CHUNK_CHARS], each with
 *   the text around it (previous_text / next_text) so the voice stays the same across the joins, and the MP3 parts are
 *   joined. One long request lets the voice drift over a multi-minute story and the API takes at most 10,000 characters.
 *
 * API key: create at [elevenlabs.io → Settings → API Keys](https://elevenlabs.io/app/settings/api-keys),
 * set ELEVENLABS_API_KEY in backend .env. Plan must include Instant Voice Cloning.
 * For "create from reference on first use": S3 + VoiceReferenceStoragePort (app.storage.type=s3).
 */
@Component
@ConditionalOnExpression("!'\${app.voice-cloning.eleven-labs-api-key:}'.trim().isEmpty()")
class ElevenLabsVoiceCloningAdapter(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    // Must match application.yml keys: eleven-labs-api-key / eleven-labs-base-url (kebab-case).
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.eleven-labs-api-key:}") apiKeyRaw: String,
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.eleven-labs-base-url:https://api.elevenlabs.io}") private val baseUrl: String,
    // Likeness of cloned narration (TTS voice_settings); see application.yml app.voice-cloning.eleven-labs-*.
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.eleven-labs-stability:0.5}") private val stability: Double = 0.5,
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.eleven-labs-similarity-boost:0.9}") private val similarityBoost: Double = 0.9,
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.eleven-labs-style:0.0}") private val style: Double = 0.0,
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.eleven-labs-speaker-boost:true}") private val speakerBoost: Boolean = true,
    // Speaking pace of cloned narration, 0.7..1.2 (1.0 = the pace of the parent's recording).
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.eleven-labs-speed:1.0}") private val speed: Double = 1.0,
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.eleven-labs-model:eleven_multilingual_v2}") modelRaw: String = DEFAULT_MODEL,
    // Clean the parent's recording when the clone is made, so room noise and echo don't become part of the voice.
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.eleven-labs-remove-background-noise:true}") private val removeBackgroundNoise: Boolean = true
) : ElevenLabsVoiceCloningPort {

    private val log = LoggerFactory.getLogger(javaClass)
    /** Trimmed so .env whitespace or newline doesn't cause 401. */
    private val apiKey: String = apiKeyRaw.trim()
    private val model: String = modelRaw.trim().ifBlank { DEFAULT_MODEL }
    private data class ProviderFailure(val at: Instant, val kind: String, val message: String)
    private val failuresByVoiceId: ConcurrentHashMap<String, ProviderFailure> = ConcurrentHashMap()
    private val authFailureWindow: Duration = Duration.ofHours(6)

    /**
     * voice_settings for cloned (parent) voices: app.voice-cloning.eleven-labs-*. Instant clones sound most natural
     * around stability 0.5 / similarity 0.75–0.8 / speed 1.0; the story narrator passes its own settings.
     */
    private val cloneSettings = ElevenLabsVoiceSettings(
        stability = stability,
        similarityBoost = similarityBoost,
        style = style,
        speakerBoost = speakerBoost,
        speed = speed,
    )

    @PostConstruct
    fun logStartup() {
        val keyStatus = if (apiKey.isNotBlank()) "set (***${apiKey.takeLast(4.coerceAtMost(apiKey.length))})" else "missing"
        log.info(
            "ElevenLabs integration enabled: baseUrl={}, apiKey={}, model={}, removeBackgroundNoise={}. Cloned narration will use ElevenLabs when profile has reference_audio_path and S3 reference storage is configured.",
            baseUrl, keyStatus, model, removeBackgroundNoise
        )
    }

    /**
     * Creates an Instant Voice Clone via POST /v1/voices/add (IVC API).
     * Required: name, files. Optional: remove_background_noise (default false), description, labels.
     * @return voice_id from AddVoiceIvcResponseModel, or null on failure
     */
    override fun addVoice(audioBytes: ByteArray, fileName: String, name: String): String? {
        if (apiKey.isBlank()) {
            log.warn("ElevenLabs addVoice skipped: API key is blank")
            return null
        }
        return try {
            val headers = HttpHeaders().apply {
                set("xi-api-key", apiKey)
                contentType = MediaType.MULTIPART_FORM_DATA
            }
            val fileResource = object : ByteArrayResource(audioBytes) {
                override fun getFilename(): String = fileName
            }
            val filePartHeaders = HttpHeaders().apply {
                contentType = guessAudioMediaType(fileName)
            }
            val body = LinkedMultiValueMap<String, Any>().apply {
                add("name", name)
                add("files", HttpEntity(fileResource, filePartHeaders))
                add("remove_background_noise", removeBackgroundNoise)
            }
            val entity = HttpEntity(body, headers)
            val url = "$baseUrl/v1/voices/add"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, Map::class.java)
            val voiceId = (response.body?.get("voice_id") as? String)
            val requiresVerification = response.body?.get("requires_verification") as? Boolean
            if (voiceId != null) {
                log.info("ElevenLabs IVC voice created voiceId={} requires_verification={}", voiceId.take(8), requiresVerification)
            }
            voiceId
        } catch (e: org.springframework.web.client.HttpClientErrorException.Unauthorized) {
            val body = e.responseBodyAsString?.take(200) ?: ""
            log.warn(
                "ElevenLabs addVoice 401 Unauthorized. Key is invalid, expired, or your plan may not include voice cloning. Get a valid key at elevenlabs.io → Profile → API Key. Response: {}",
                if (body.isNotBlank()) body else "(none)"
            )
            null
        } catch (e: HttpClientErrorException) {
            if (e.statusCode.value() == 400) {
                val body = e.responseBodyAsString ?: ""
                if ("voice_limit_reached" in body) {
                    log.warn(
                        "ElevenLabs addVoice 400: custom voice limit reached (10/10). Delete unused voices at elevenlabs.io or upgrade plan. Cloned narration will fail until a slot is free."
                    )
                } else if ("does not have a sample" in body.lowercase()) {
                    log.warn(
                        "ElevenLabs addVoice 400: created voice has no playable sample. Ensure uploaded reference is valid MP3/WAV speech (>=10s, clear, non-empty). Response={}",
                        body.take(300)
                    )
                } else {
                    log.warn("ElevenLabs addVoice 400 Bad Request: {}", body.take(300))
                }
            } else {
                log.warn("ElevenLabs addVoice failed: {} {}", e.statusCode, e.message)
            }
            null
        } catch (e: Exception) {
            log.warn("ElevenLabs addVoice failed: {}", e.message, e)
            null
        }
    }

    override fun synthesize(text: String, voiceId: String, language: String): ByteArray? =
        synthesize(text, voiceId, language, cloneSettings)

    override fun synthesize(text: String, voiceId: String, language: String, settings: ElevenLabsVoiceSettings): ByteArray? {
        val voiceSettings = settings.toRequest()
        if (apiKey.isBlank()) {
            log.warn("ElevenLabs synthesize skipped: API key is blank")
            return null
        }
        if (text.isBlank()) return null
        val chunks = NarrationTextUtils.splitForSpeech(text, MAX_CHUNK_CHARS)
        val langCode = mapLanguageToElevenLabs(language)
        // Some ElevenLabs voices reject language_code for certain locales/voice states; once a part needs the retry
        // without it, the rest of the story is sent without it too.
        var sendLanguageCode = true
        val parts = ArrayList<ByteArray>(chunks.size)
        for ((i, chunk) in chunks.withIndex()) {
            val request = buildMap<String, Any> {
                put("text", chunk)
                put("model_id", model)
                put("voice_settings", voiceSettings)
                chunks.getOrNull(i - 1)?.let { put("previous_text", it.takeLast(CONTEXT_CHARS)) }
                chunks.getOrNull(i + 1)?.let { put("next_text", it.take(CONTEXT_CHARS)) }
            }
            var audio = if (sendLanguageCode) synthesizeInternal(voiceId, request + ("language_code" to langCode)) else null
            if (audio == null || audio.isEmpty()) {
                if (sendLanguageCode) {
                    log.warn(
                        "ElevenLabs synthesize returned empty for voiceId={} lang={}; retrying without language_code",
                        voiceId.take(8),
                        langCode
                    )
                    sendLanguageCode = false
                }
                audio = synthesizeInternal(voiceId, request)
            }
            if (audio == null || audio.isEmpty()) {
                // A story with a missing part must not be saved: the caller marks it failed and it is retried later.
                log.warn("ElevenLabs synthesize failed for part {}/{} voiceId={} lang={}", i + 1, chunks.size, voiceId.take(8), langCode)
                return null
            }
            parts += audio
        }
        if (parts.size > 1) {
            log.info("ElevenLabs synthesize joined {} parts voiceId={} lang={} chars={}", parts.size, voiceId.take(8), langCode, text.length)
        }
        // Every part is MP3 at the same rate and bitrate (output_format); the joiner keeps only their audio frames so
        // part headers don't end up mid-story (they made readers and players see only part 1's length).
        return Mp3Joiner.join(parts)
    }

    /** DELETE /v1/voices/{voice_id}. A 404 means the voice is already gone, which counts as deleted. */
    override fun deleteVoice(voiceId: String): Boolean {
        if (apiKey.isBlank() || voiceId.isBlank()) return false
        return try {
            val headers = HttpHeaders().apply { set("xi-api-key", apiKey) }
            restTemplate.exchange("$baseUrl/v1/voices/$voiceId", HttpMethod.DELETE, HttpEntity<Unit>(headers), String::class.java)
            failuresByVoiceId.remove(voiceId)
            log.info("ElevenLabs voice deleted voiceId={}", voiceId.take(8))
            true
        } catch (e: HttpClientErrorException.NotFound) {
            log.info("ElevenLabs voice already deleted voiceId={}", voiceId.take(8))
            true
        } catch (e: Exception) {
            log.warn("ElevenLabs deleteVoice failed voiceId={}: {}", voiceId.take(8), e.message)
            false
        }
    }

    override fun hadRecentAuthFailure(voiceId: String): Boolean {
        val failure = failuresByVoiceId[voiceId] ?: return false
        return failure.kind == "auth" && Duration.between(failure.at, Instant.now()) <= authFailureWindow
    }

    override fun hadRecentQuotaFailure(voiceId: String): Boolean {
        val failure = failuresByVoiceId[voiceId] ?: return false
        return failure.kind == "quota" && Duration.between(failure.at, Instant.now()) <= authFailureWindow
    }

    override fun recentFailureMessage(voiceId: String): String? {
        val failure = failuresByVoiceId[voiceId] ?: return null
        if (Duration.between(failure.at, Instant.now()) > authFailureWindow) return null
        return failure.message
    }

    private fun synthesizeInternal(voiceId: String, request: Map<String, Any>): ByteArray? {
        return try {
            val headers = HttpHeaders().apply {
                set("xi-api-key", apiKey)
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)
            val url = "$baseUrl/v1/text-to-speech/$voiceId?output_format=mp3_44100_128"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, ByteArray::class.java)
            val body = response.body
            if (body != null && body.isNotEmpty()) {
                failuresByVoiceId.remove(voiceId)
            }
            body
        } catch (e: org.springframework.web.client.HttpClientErrorException.Unauthorized) {
            val body = (e.responseBodyAsString ?: "").take(400)
            if (body.contains("quota_exceeded", ignoreCase = true)) {
                val detail = if (body.isNotBlank()) body else "credits exhausted"
                failuresByVoiceId[voiceId] = ProviderFailure(
                    at = Instant.now(),
                    kind = "quota",
                    message = "ElevenLabs quota exceeded for this voice/profile. $detail"
                )
                log.warn("ElevenLabs synthesize 401 quota_exceeded for voiceId={}: {}", voiceId.take(8), detail)
            } else {
                failuresByVoiceId[voiceId] = ProviderFailure(
                    at = Instant.now(),
                    kind = "auth",
                    message = "ElevenLabs auth/permissions failure (401). Check API key/workspace permissions."
                )
                log.warn(
                    "ElevenLabs synthesize 401 for voiceId={} — auth/permissions issue. Response={}",
                    voiceId.take(8),
                    if (body.isNotBlank()) body else "(none)"
                )
            }
            null
        } catch (e: HttpClientErrorException) {
            log.warn(
                "ElevenLabs synthesize {} for voiceId={}: {}",
                e.statusCode,
                voiceId.take(8),
                (e.responseBodyAsString ?: e.message ?: "request failed").take(300)
            )
            null
        } catch (e: Exception) {
            log.warn("ElevenLabs synthesize failed voiceId={}: {}", voiceId.take(8), e.message, e)
            null
        }
    }

    private fun mapLanguageToElevenLabs(lang: String): String {
        val normalized = lang.trim().lowercase().take(10).substringBefore('-')
        return when (normalized) {
            "ta", "tamil" -> "ta"
            "hi", "hindi" -> "hi"
            "te", "telugu" -> "te"
            "kn", "kannada" -> "kn"
            "ml", "malayalam" -> "ml"
            "bn", "bengali" -> "bn"
            "en", "english" -> "en"
            else -> "en"
        }
    }

    private fun guessAudioMediaType(fileName: String): MediaType {
        val lower = fileName.trim().lowercase()
        return when {
            lower.endsWith(".wav") -> MediaType.parseMediaType("audio/wav")
            lower.endsWith(".m4a") -> MediaType.parseMediaType("audio/mp4")
            lower.endsWith(".aac") -> MediaType.parseMediaType("audio/aac")
            lower.endsWith(".ogg") -> MediaType.parseMediaType("audio/ogg")
            else -> MediaType.parseMediaType("audio/mpeg")
        }
    }

    private fun ElevenLabsVoiceSettings.toRequest(): Map<String, Any> = mapOf(
        "stability" to stability.coerceIn(0.0, 1.0),
        "similarity_boost" to similarityBoost.coerceIn(0.0, 1.0),
        "style" to style.coerceIn(0.0, 1.0),
        "use_speaker_boost" to speakerBoost,
        "speed" to speed.coerceIn(0.7, 1.2),
    )

    companion object {
        const val DEFAULT_MODEL = "eleven_multilingual_v2"

        /** Longest text sent in one request; short enough that the voice can't drift within a part. */
        const val MAX_CHUNK_CHARS = 1_000

        /** Text from the neighbouring part sent as previous_text / next_text (context only, not spoken). */
        private const val CONTEXT_CHARS = 300
    }
}
