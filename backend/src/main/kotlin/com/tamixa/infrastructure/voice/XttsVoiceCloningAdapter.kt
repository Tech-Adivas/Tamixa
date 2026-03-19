package com.tamixa.infrastructure.voice

import com.tamixa.application.port.voice.SelfHostedVoiceCloningPort
import com.tamixa.application.port.voice.VoiceReferenceStoragePort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.cdn.S3SignedUrlGenerator
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.util.Base64

/**
 * Self-hosted XTTS-style voice cloning adapter.
 *
 * Expects a Python service exposing POST /synthesize:
 *   { "text", "reference_path", "language", "reference_url" (optional), "reference_audio_base64" (optional) }
 *
 * Sends reference_audio_base64 so the service can synthesize the story (not the stub). Prefers
 * VoiceReferenceStoragePort.getReferenceAudio; falls back to S3 GetObject when path looks like an S3 key.
 *
 * Configure with app.voice-cloning.xtts-base-url, for example:
 *   http://localhost:9000 or https://xtts.internal
 */
@Component
@ConditionalOnProperty(name = ["app.voice-cloning.xtts-base-url"])
class XttsVoiceCloningAdapter(
    @Qualifier("xttsRestTemplate") private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val appProperties: AppProperties,
    @Autowired(required = false) private val s3SignedUrlGenerator: S3SignedUrlGenerator?,
    @Autowired(required = false) private val s3Client: S3Client?,
    @Autowired(required = false) private val voiceReferenceStorage: VoiceReferenceStoragePort?
) : SelfHostedVoiceCloningPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val bucket: String get() = appProperties.storage.effectiveS3Bucket

    /**
     * Coqui XTTS v2 only accepts a fixed language set. Tamil (ta) is passed through so that
     * Tamil-capable XTTS forks work; if the service returns 500 for ta, use ElevenLabs for Tamil.
     * Other unsupported Indic codes are mapped to a supported one so synthesis runs (prosody may differ).
     */
    private val xttsSupportedLanguages = setOf(
        "en", "es", "fr", "de", "it", "pt", "pl", "tr", "ru", "nl", "cs", "ar", "zh-cn", "hu", "ko", "ja", "hi",
    )

    /** Indic codes not in XTTS list — map to hi so synthesis runs. Tamil (ta) is NOT included: pass ta through for Tamil-capable XTTS or use ElevenLabs. */
    private val mapIndicToHi = setOf("te", "ml", "kn", "gu", "pa", "bn", "mr", "ur", "si")

    private fun normalizeLanguageForXtts(language: String): String {
        val code = language.lowercase().trim().take(5).substringBefore('-')
        val full = language.lowercase().trim()
        if (full == "zh-cn" || full == "zh_cn") return "zh-cn"
        if (xttsSupportedLanguages.contains(code)) return code
        // Pass Tamil through: ElevenLabs supports ta; if XTTS is used and returns 500, caller should use ElevenLabs for Tamil.
        if (code == "ta") return "ta"
        if (mapIndicToHi.contains(code)) {
            log.warn("XTTS does not support language={}; using hi for synthesis (text is unchanged).", language)
            return "hi"
        }
        log.warn("XTTS does not support language={}; using en for synthesis (text is unchanged).", language)
        return "en"
    }

    override fun synthesize(text: String, referencePath: String, language: String): ByteArray? {
        val baseUrl = appProperties.voiceCloning.xttsBaseUrl
        if (baseUrl.isBlank()) {
            log.warn("XTTS synthesize skipped: app.voice-cloning.xtts-base-url is blank")
            return null
        }
        if (text.isBlank()) {
            log.warn("XTTS synthesize skipped: empty text")
            return null
        }
        if (referencePath.isBlank()) {
            log.warn("XTTS synthesize skipped: empty referencePath")
            return null
        }

        // XTTS does not support Tamil; do not attempt (would 500). Cloned-voice Tamil must use ElevenLabs.
        val langCode = language.trim().lowercase().take(5).substringBefore('-')
        if (langCode == "ta" || langCode == "tamil") {
            log.warn("XTTS does not support Tamil ({}). Use ElevenLabs for cloned-voice Tamil: set ELEVENLABS_API_KEY.", language)
            return null
        }

        // Keep payload size reasonable; XTTS service can further chunk if needed.
        val maxChars = 10_000
        val input = text.take(maxChars)
        if (input.length < text.length) {
            log.debug("XTTS: truncated input from {} to {} chars", text.length, maxChars)
        }

        return try {
            val xttsLang = normalizeLanguageForXtts(language)
            val request = mutableMapOf<String, Any>(
                "text" to input,
                "reference_path" to referencePath,
                "language" to xttsLang
            )
            // So XTTS service can log user's choice (e.g. requested=ta) when we send normalized (e.g. hi)
            if (language != xttsLang) {
                request["requested_language"] = language
            }
            s3SignedUrlGenerator?.signUrl(referencePath)?.toString()?.takeIf { it.isNotBlank() }?.let { url ->
                request["reference_url"] = url
                log.debug("XTTS request includes reference_url for key={}", referencePath)
            }
            // Always send reference bytes so XTTS synthesizes the story instead of returning stub (tamixa.mp3).
            val refBytes = voiceReferenceStorage?.getReferenceAudio(referencePath)
                ?: if (s3Client != null && referencePath.isNotBlank() && !referencePath.startsWith("http")) {
                    try {
                        val getReq = GetObjectRequest.builder().bucket(bucket).key(referencePath).build()
                        val response: ResponseInputStream<*> = s3Client.getObject(getReq)
                        response.readAllBytes()
                    } catch (e: Exception) {
                        log.warn("XTTS: could not read reference from S3 key={}: {}", referencePath, e.message)
                        null
                    }
                } else null
            if (refBytes != null && refBytes.isNotEmpty()) {
                request["reference_audio_base64"] = Base64.getEncoder().encodeToString(refBytes)
                log.debug("XTTS request includes reference_audio_base64 for path={} ({} bytes)", referencePath, refBytes.size)
            } else {
                log.warn("XTTS request has no reference audio (path={}); service will return stub. Ensure voice storage is configured and path is readable.", referencePath)
            }
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(objectMapper.writeValueAsString(request), headers)
            val url = baseUrl.trimEnd('/') + "/synthesize"
            val response = restTemplate.exchange(url, HttpMethod.POST, entity, ByteArray::class.java)
            val bytes = response.body
            val synthStatus = response.headers.getFirst("X-Synthesis-Status")
            val synthChunks = response.headers.getFirst("X-Synthesis-Chunks")
            if (bytes == null || bytes.isEmpty()) {
                log.warn("XTTS synthesis did not complete: empty body referencePath={} (status header={})", referencePath, synthStatus)
                null
            } else {
                when (synthStatus?.lowercase()) {
                    "completed" -> log.info(
                        "XTTS synthesis completed: {} bytes, chunks={}, lang={} (requested={}), referencePath={}",
                        bytes.size, synthChunks ?: "?", xttsLang, language, referencePath
                    )
                    "stub" -> log.warn(
                        "XTTS returned stub (no reference); {} bytes — not real cloned narration referencePath={}",
                        bytes.size, referencePath
                    )
                    else -> log.warn(
                        "XTTS synthesis unclear: {} bytes, X-Synthesis-Status={} referencePath={} — verify XTTS service",
                        bytes.size, synthStatus, referencePath
                    )
                }
                bytes
            }
        } catch (e: HttpStatusCodeException) {
            val status = e.statusCode.value()
            val body = e.responseBodyAsString?.take(500) ?: e.message ?: "XTTS service error"
            log.warn("XTTS synthesize failed referencePath={} status={} body={}", referencePath, status, body.take(200))
            if (status == 500 && language.trim().lowercase().startsWith("ta")) {
                log.warn("XTTS returned 500 for Tamil (ta). Tamil cloned voice requires ElevenLabs: set ELEVENLABS_API_KEY in .env and restart backend (XTTS does not support Tamil).")
            }
            if (status == 503) {
                val detail = e.responseBodyAsString?.let { parseDetail(it) } ?: body
                throw XttsUnavailableException(detail, e)
            }
            null
        } catch (e: ResourceAccessException) {
            val msg = e.message ?: e.cause?.message ?: "Unknown error"
            val timeoutHint = if (msg.contains("timed out", ignoreCase = true))
                " (increase XTTS_TIMEOUT_SECONDS if synthesis is slow, e.g. 600)"
            else ""
            log.warn("XTTS synthesize failed for referencePath={}: {}{}", referencePath, msg, timeoutHint, e)
            null
        } catch (e: Exception) {
            val msg = e.message ?: e.cause?.message ?: "Unknown error"
            log.warn("XTTS synthesize failed for referencePath={}: {}", referencePath, msg, e)
            null
        }
    }

    private fun parseDetail(json: String): String? {
        return try {
            val tree = objectMapper.readTree(json)
            tree.get("detail")?.asText() ?: tree.get("message")?.asText()
        } catch (_: Exception) {
            null
        }
    }
}

