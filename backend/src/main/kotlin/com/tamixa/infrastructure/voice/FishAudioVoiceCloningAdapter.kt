package com.tamixa.infrastructure.voice

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.voice.FishAudioVoiceCloningPort
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
 * Fish Audio: POST /model (multipart) then POST /v1/tts (JSON, Bearer + model header).
 * API key: https://fish.audio/app/api-keys/ → FISH_AUDIO_API_KEY
 */
@Component
@ConditionalOnExpression("!'\${app.voice-cloning.fish-audio-api-key:}'.trim().isEmpty()")
class FishAudioVoiceCloningAdapter(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.fish-audio-api-key:}") apiKeyRaw: String,
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.fish-audio-base-url:https://api.fish.audio}") private val baseUrl: String,
    @org.springframework.beans.factory.annotation.Value("\${app.voice-cloning.fish-audio-tts-model:s2-pro}") private val ttsModel: String
) : FishAudioVoiceCloningPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val apiKey: String = apiKeyRaw.trim()
    private data class ProviderFailure(val at: Instant, val kind: String, val message: String)
    private val failuresByModelId: ConcurrentHashMap<String, ProviderFailure> = ConcurrentHashMap()
    private val failureWindow: Duration = Duration.ofHours(6)

    @PostConstruct
    fun logStartup() {
        val keyStatus = if (apiKey.isNotBlank()) "set (***${apiKey.takeLast(4.coerceAtMost(apiKey.length))})" else "missing"
        log.info(
            "Fish Audio integration enabled: baseUrl={}, ttsModel={}, apiKey={}. Use VOICE_CLONING_PROVIDER=fishaudio or Google + VOICE_CLONING_ALLOW_FISH_AUDIO_FALLBACK=true.",
            baseUrl.trimEnd('/'),
            ttsModel,
            keyStatus
        )
    }

    override fun createModelFromSample(audioBytes: ByteArray, fileName: String, title: String): String? {
        if (apiKey.isBlank()) {
            log.warn("Fish Audio createModel skipped: API key is blank")
            return null
        }
        return try {
            val headers = HttpHeaders().apply {
                setBearerAuth(apiKey)
            }
            val fileResource = object : ByteArrayResource(audioBytes) {
                override fun getFilename(): String = fileName
            }
            val filePartHeaders = HttpHeaders().apply {
                contentType = guessAudioMediaType(fileName)
            }
            val body = LinkedMultiValueMap<String, Any>().apply {
                add("type", "tts")
                add("title", title.take(200))
                add("train_mode", "fast")
                add("visibility", "private")
                add("voices", HttpEntity(fileResource, filePartHeaders))
            }
            val entity = HttpEntity(body, headers)
            val url = "${baseUrl.trimEnd('/')}/model"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, Map::class.java)
            val id = response.body?.get("_id") as? String
            if (id != null) {
                log.info("Fish Audio model created modelId={}", id.take(12))
            } else {
                log.warn("Fish Audio createModel: missing _id in response")
            }
            id
        } catch (e: HttpClientErrorException.Unauthorized) {
            log.warn(
                "Fish Audio createModel 401 Unauthorized. Check FISH_AUDIO_API_KEY. Response: {}",
                (e.responseBodyAsString ?: "").take(200)
            )
            null
        } catch (e: HttpClientErrorException) {
            val code = e.statusCode.value()
            val snippet = (e.responseBodyAsString ?: e.message ?: "").take(400)
            if (code == 402) {
                log.warn("Fish Audio createModel 402 (billing/credits): {}", snippet)
            } else {
                log.warn("Fish Audio createModel failed {}: {}", code, snippet)
            }
            null
        } catch (e: Exception) {
            log.warn("Fish Audio createModel failed: {}", e.message, e)
            null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    override fun synthesize(text: String, modelId: String, language: String): ByteArray? {
        if (apiKey.isBlank()) {
            log.warn("Fish Audio synthesize skipped: API key is blank")
            return null
        }
        if (text.isBlank()) return null
        val plainText = text.take(10_000)
        return try {
            val headers = HttpHeaders().apply {
                setBearerAuth(apiKey)
                contentType = MediaType.APPLICATION_JSON
                set("model", ttsModel.trim().ifBlank { "s2-pro" })
            }
            val payload = linkedMapOf<String, Any?>(
                "text" to plainText,
                "reference_id" to modelId,
                "format" to "mp3",
                "sample_rate" to 44100,
                "mp3_bitrate" to 128,
                "normalize" to true
            )
            val entity = HttpEntity(objectMapper.writeValueAsString(payload), headers)
            val url = "${baseUrl.trimEnd('/')}/v1/tts"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, ByteArray::class.java)
            val body = response.body
            if (body != null && body.isNotEmpty()) {
                failuresByModelId.remove(modelId)
            }
            body
        } catch (e: HttpClientErrorException.Unauthorized) {
            recordFailure(modelId, "auth", "Fish Audio auth failure (401). Check FISH_AUDIO_API_KEY.")
            log.warn(
                "Fish Audio synthesize 401 for modelId={}: {}",
                modelId.take(12),
                (e.responseBodyAsString ?: "").take(200)
            )
            null
        } catch (e: HttpClientErrorException) {
            val code = e.statusCode.value()
            val bodyStr = (e.responseBodyAsString ?: "").take(500)
            if (code == 402 || bodyStr.contains("payment", ignoreCase = true) || bodyStr.contains("credit", ignoreCase = true)) {
                recordFailure(modelId, "quota", "Fish Audio billing or credits issue ($code). $bodyStr")
                log.warn("Fish Audio synthesize {} (billing/credits) modelId={}: {}", code, modelId.take(12), bodyStr)
            } else {
                log.warn("Fish Audio synthesize {} modelId={}: {}", code, modelId.take(12), bodyStr)
            }
            null
        } catch (e: Exception) {
            log.warn("Fish Audio synthesize failed modelId={}: {}", modelId.take(12), e.message, e)
            null
        }
    }

    override fun hadRecentAuthFailure(modelId: String): Boolean {
        val failure = failuresByModelId[modelId] ?: return false
        return failure.kind == "auth" && Duration.between(failure.at, Instant.now()) <= failureWindow
    }

    override fun hadRecentQuotaFailure(modelId: String): Boolean {
        val failure = failuresByModelId[modelId] ?: return false
        return failure.kind == "quota" && Duration.between(failure.at, Instant.now()) <= failureWindow
    }

    override fun recentFailureMessage(modelId: String): String? {
        val failure = failuresByModelId[modelId] ?: return null
        if (Duration.between(failure.at, Instant.now()) > failureWindow) return null
        return failure.message
    }

    private fun recordFailure(modelId: String, kind: String, message: String) {
        failuresByModelId[modelId] = ProviderFailure(at = Instant.now(), kind = kind, message = message)
    }

    private fun guessAudioMediaType(fileName: String): MediaType {
        val lower = fileName.trim().lowercase()
        return when {
            lower.endsWith(".wav") -> MediaType.parseMediaType("audio/wav")
            lower.endsWith(".m4a") -> MediaType.parseMediaType("audio/mp4")
            lower.endsWith(".flac") -> MediaType.parseMediaType("audio/flac")
            lower.endsWith(".ogg") -> MediaType.parseMediaType("audio/ogg")
            else -> MediaType.parseMediaType("audio/mpeg")
        }
    }
}
