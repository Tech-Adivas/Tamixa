package com.tamixa.infrastructure.voice

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.voice.GoogleCloudVoiceCloningPort
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.Base64
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Qualifier

/**
 * Google Cloud Chirp 3 Instant Custom Voice adapter.
 * Creates a voice cloning key from reference + consent audio, then synthesizes TTS with that key.
 * Requires VOICE_CLONING_PROVIDER=google and GOOGLE_CLOUD_TTS_API_KEY (or app.voice-cloning.google-cloud-api-key).
 */
@Component
@ConditionalOnProperty(name = ["app.voice-cloning.provider"], havingValue = "google")
class GoogleCloudVoiceCloningAdapter(
    @Qualifier("googleTtsRestTemplate") private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val appProperties: AppProperties
) : GoogleCloudVoiceCloningPort {

    private val log = LoggerFactory.getLogger(javaClass)

    private val apiKey: String
        get() = appProperties.voiceCloning.googleCloudApiKey.ifBlank {
            appProperties.narration.googleTtsApiKey
        }.trim()

    @PostConstruct
    fun logStartup() {
        if (apiKey.isBlank()) {
            log.warn("Google Cloud voice cloning skipped: API key not configured")
            return
        }
        log.info("Google Cloud voice cloning enabled (Chirp 3 Instant Custom Voice)")
    }

    override fun createVoiceCloningKey(
        referenceAudioBytes: ByteArray,
        consentAudioBytes: ByteArray,
        languageCode: String
    ): String? {
        if (apiKey.isBlank()) return null
        if (referenceAudioBytes.isEmpty() || consentAudioBytes.isEmpty()) return null
        val consentScript = CONSENT_SCRIPTS[languageCode] ?: CONSENT_SCRIPTS["en-US"]!!
        val requestBody = mapOf(
            "reference_audio" to mapOf(
                "audio_config" to mapOf("audio_encoding" to "MP3"),
                "content" to Base64.getEncoder().encodeToString(referenceAudioBytes)
            ),
            "voice_talent_consent" to mapOf(
                "audio_config" to mapOf("audio_encoding" to "MP3"),
                "content" to Base64.getEncoder().encodeToString(consentAudioBytes)
            ),
            "consent_script" to consentScript,
            "language_code" to languageCode
        )
        val headers = HttpHeaders().apply {
            set("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            set("X-Goog-Api-Key", apiKey)
        }
        return try {
            val entity = HttpEntity(objectMapper.writeValueAsString(requestBody), headers)
            val url = "https://texttospeech.googleapis.com/v1beta1/voices:generateVoiceCloningKey"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, Map::class.java)
            val body = response.body ?: return null
            (body["voiceCloningKey"] as? String)?.also {
                log.info("Google voice cloning key created for lang={}", languageCode)
            }
        } catch (e: Exception) {
            log.warn("Google voice cloning key creation failed: {} (lang={})", e.message, languageCode, e)
            null
        }
    }

    override fun synthesize(text: String, voiceCloningKey: String, language: String): ByteArray? {
        if (apiKey.isBlank() || voiceCloningKey.isBlank()) return null
        val languageCode = normalizeLanguageCode(language)
        val requestBody = mapOf(
            "input" to mapOf("text" to text),
            "voice" to mapOf(
                "language_code" to languageCode,
                "voice_clone" to mapOf("voice_cloning_key" to voiceCloningKey)
            ),
            "audioConfig" to mapOf(
                "audioEncoding" to "MP3"
            )
        )
        val headers = HttpHeaders().apply {
            set("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            set("X-Goog-Api-Key", apiKey)
        }
        return try {
            val entity = HttpEntity(objectMapper.writeValueAsString(requestBody), headers)
            val url = "https://texttospeech.googleapis.com/v1beta1/text:synthesize"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, Map::class.java)
            val body = response.body ?: return null
            val audioContent = body["audioContent"] as? String ?: return null
            Base64.getDecoder().decode(audioContent)
        } catch (e: Exception) {
            log.warn("Google voice cloning synthesize failed: {} (lang={})", e.message, language, e)
            null
        }
    }

    private fun normalizeLanguageCode(language: String): String {
        val code = language.trim().lowercase().take(10).substringBefore('-')
        return when (code) {
            "ta", "tamil" -> "ta-IN"
            "hi", "hindi" -> "hi-IN"
            "te", "telugu" -> "te-IN"
            "en", "english" -> "en-US"
            else -> when {
                language.contains("-") -> language.trim().take(10)
                else -> "$code-IN"
            }
        }
    }

    companion object {
        private val CONSENT_SCRIPTS = mapOf(
            "en-US" to "I am the owner of this voice and I consent to Google using this voice to create a synthetic voice model.",
            "en-GB" to "I am the owner of this voice and I consent to Google using this voice to create a synthetic voice model.",
            "en-AU" to "I am the owner of this voice and I consent to Google using this voice to create a synthetic voice model.",
            "en-IN" to "I am the owner of this voice and I consent to Google using this voice to create a synthetic voice model."
        )
    }
}
