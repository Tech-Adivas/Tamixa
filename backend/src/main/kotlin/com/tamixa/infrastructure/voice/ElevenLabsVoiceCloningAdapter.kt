package com.tamixa.infrastructure.voice

import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
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
 * - TTS: POST /v1/text-to-speech/{voice_id}
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
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.eleven-labs-base-url:https://api.elevenlabs.io}") private val baseUrl: String
) : ElevenLabsVoiceCloningPort {

    private val log = LoggerFactory.getLogger(javaClass)
    /** Trimmed so .env whitespace or newline doesn't cause 401. */
    private val apiKey: String = apiKeyRaw.trim()
    private data class ProviderFailure(val at: Instant, val kind: String, val message: String)
    private val failuresByVoiceId: ConcurrentHashMap<String, ProviderFailure> = ConcurrentHashMap()
    private val authFailureWindow: Duration = Duration.ofHours(6)

    @PostConstruct
    fun logStartup() {
        val keyStatus = if (apiKey.isNotBlank()) "set (***${apiKey.takeLast(4.coerceAtMost(apiKey.length))})" else "missing"
        log.info(
            "ElevenLabs integration enabled: baseUrl={}, apiKey={}. Cloned narration will use ElevenLabs when profile has reference_audio_path and S3 reference storage is configured.",
            baseUrl, keyStatus
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
                add("remove_background_noise", false)
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

    override fun synthesize(text: String, voiceId: String, language: String): ByteArray? {
        if (apiKey.isBlank()) {
            log.warn("ElevenLabs synthesize skipped: API key is blank")
            return null
        }
        if (text.isBlank()) return null
        val plainText = text.take(10_000)
        val langCode = mapLanguageToElevenLabs(language)
        val firstAttempt = synthesizeInternal(
            voiceId = voiceId,
            request = mapOf(
                "text" to plainText,
                "model_id" to "eleven_multilingual_v2",
                "language_code" to langCode
            )
        )
        if (firstAttempt != null && firstAttempt.isNotEmpty()) {
            return firstAttempt
        }

        // Some ElevenLabs voices reject language_code for certain locales/voice states.
        // Retry with model-only payload so provider can auto-detect language from text.
        log.warn(
            "ElevenLabs synthesize returned empty for voiceId={} lang={}; retrying without language_code",
            voiceId.take(8),
            langCode
        )
        val fallbackAttempt = synthesizeInternal(
            voiceId = voiceId,
            request = mapOf(
                "text" to plainText,
                "model_id" to "eleven_multilingual_v2"
            )
        )
        return if (fallbackAttempt != null && fallbackAttempt.isNotEmpty()) fallbackAttempt else null
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
}
