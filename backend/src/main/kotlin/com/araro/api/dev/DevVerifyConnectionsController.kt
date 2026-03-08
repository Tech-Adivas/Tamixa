package com.araro.api.dev

import com.araro.api.ApiVersion
import com.araro.application.port.CoverVideoGenerationPort
import com.araro.application.narration.AudioStorageService
import com.araro.application.narration.StoryProcessingService
import com.araro.application.narration.TokenUsageService
import com.araro.application.translation.TranslationService
import com.araro.application.narration.TTSService
import com.araro.infrastructure.config.AppProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestTemplate
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request

/**
 * Dev-only endpoints to verify external service connections and the audio translation pipeline.
 * - GET /api/v1/dev/verify-connections – OpenAI, S3
 * - GET /api/v1/dev/verify-audio-translation – Translation (ta→en) → TTS → S3 upload
 * - GET /api/v1/dev/verify-google-tts – Google Cloud TTS connectivity (when NARRATION_TTS_PROVIDER=google)
 * - GET /api/v1/dev/tts-status – Current TTS provider and whether Google TTS is active for story processing
 * - POST /api/v1/dev/trigger-pipeline/{masterStoryId} – Manually trigger narration pipeline when stuck at PENDING
 */
@RestController
@RequestMapping("${ApiVersion.V1}/dev")
@Profile("dev")
class DevVerifyConnectionsController(
    private val restTemplate: RestTemplate,
    private val appProperties: AppProperties,
    private val s3ClientProvider: ObjectProvider<S3Client>,
    private val translationService: TranslationService,
    private val ttsService: TTSService,
    private val storyProcessingService: StoryProcessingService,
    private val audioStorageProvider: ObjectProvider<AudioStorageService>,
    private val tokenUsageService: TokenUsageService,
    private val coverVideoGenerationProvider: ObjectProvider<CoverVideoGenerationPort>,
    @Value("\${app.narration.tts-provider:simulated}") private val ttsProvider: String,
    @Value("\${app.narration.google-tts-api-key:}") private val googleTtsApiKey: String,
    @Value("\${app.sora.enabled:false}") private val soraEnabled: Boolean,
    @Value("\${app.sora.model:sora-2}") private val soraModel: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/verify-connections")
    fun verifyConnections(): ResponseEntity<Map<String, Any>> {
        log.info("Verify connections request (OpenAI, S3)")
        val results = mutableMapOf<String, Map<String, Any>>()

        // OpenAI
        val openaiResult = verifyOpenAI()
        results["openai"] = openaiResult

        // S3 (only when storage type is s3)
        val s3Result = if (appProperties.storage.type == "s3") {
            verifyS3()
        } else {
            mapOf(
                "status" to "SKIPPED",
                "message" to "Storage type is ${appProperties.storage.type}, S3 not configured"
            )
        }
        results["s3"] = s3Result

        // Sora (cover video): config and bean presence; actual API access is tested when generating a cover
        results["sora"] = mapOf(
            "enabled" to soraEnabled,
            "model" to soraModel,
            "beanPresent" to (coverVideoGenerationProvider.getIfAvailable() != null),
            "note" to "Videos API may require separate Sora access. Trigger 'Generate AI Cover' on a curated story to test."
        )

        val allOk = (openaiResult["status"] == "OK") &&
            (s3Result["status"] == "OK" || s3Result["status"] == "SKIPPED")
        val status = if (allOk) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
        log.info("Verify connections complete openai={} s3={} soraEnabled={} overall={}", openaiResult["status"], s3Result["status"], soraEnabled, if (allOk) "OK" else "DEGRADED")

        val soraInfo = results["sora"] as Map<String, Any>
        return ResponseEntity
            .status(status)
            .body(
                mapOf<String, Any>(
                    "openai" to openaiResult,
                    "s3" to s3Result,
                    "sora" to soraInfo,
                    "overall" to if (allOk) "OK" else "DEGRADED"
                )
            )
    }

    /**
     * Validates the full audio translation pipeline: Tamil → Translation (ta→en) → TTS → S3 upload.
     * Use to verify OpenAI translation, TTS, and S3 storage work together.
     */
    @GetMapping("/verify-audio-translation")
    fun verifyAudioTranslation(): ResponseEntity<Map<String, Any>> {
        log.info("Verify audio translation pipeline (translation → TTS → S3)")
        val results = mutableMapOf<String, Any>()
        var translationResult: TranslationService.TranslationResult? = null
        var ttsBytes: ByteArray? = null

        // Step 1: Translation (Tamil → English)
        val translationStatus = try {
            val r = translationService.translateIfNeeded(
                sourceLang = "ta",
                targetLang = "en",
                title = null,
                content = "வணக்கம்",
                moral = null
            )
            translationResult = r
            mapOf(
                "status" to "OK",
                "translated" to r.translated,
                "contentLength" to r.content.length,
                "preview" to r.content.take(50)
            )
        } catch (e: Exception) {
            mapOf("status" to "ERROR", "message" to (e.message ?: "Unknown error"))
        }
        results["translation"] = translationStatus

        // Step 2: TTS (synthesize translated or fallback text)
        val textForTts = (translationResult?.content)?.takeIf { it.isNotBlank() } ?: "Hello"
        val ssml = "<speak>$textForTts</speak>"
        val ttsStatus = try {
            ttsBytes = ttsService.synthesize(ssml, "en", "default")
            if (ttsBytes != null && ttsBytes.isNotEmpty()) {
                mapOf("status" to "OK", "bytes" to ttsBytes.size)
            } else {
                mapOf("status" to "ERROR", "message" to "TTS returned empty")
            }
        } catch (e: Exception) {
            mapOf("status" to "ERROR", "message" to (e.message ?: "Unknown error"))
        }
        results["tts"] = ttsStatus

        // Step 3: S3 upload (if storage configured and TTS succeeded)
        val storage = audioStorageProvider.getIfAvailable()
        val s3UploadStatus = if (storage != null && ttsBytes != null && ttsBytes.isNotEmpty() && appProperties.storage.type == "s3") {
            try {
                val key = storage.uploadNarrationAudio(0L, "verify-test", "default", ttsBytes)
                mapOf("status" to "OK", "key" to key)
            } catch (e: Exception) {
                mapOf("status" to "ERROR", "message" to (e.message ?: "Unknown error"))
            }
        } else {
            mapOf(
                "status" to "SKIPPED",
                "message" to when {
                    appProperties.storage.type != "s3" -> "Storage type is ${appProperties.storage.type}"
                    ttsBytes == null -> "TTS failed, skip upload"
                    else -> "AudioStorageService not available"
                }
            )
        }
        results["s3Upload"] = s3UploadStatus

        val allOk = (translationStatus["status"] == "OK") &&
            (ttsStatus["status"] == "OK") &&
            (s3UploadStatus["status"] == "OK" || s3UploadStatus["status"] == "SKIPPED")
        val status = if (allOk) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
        log.info("Verify audio translation complete translation={} tts={} s3Upload={}",
            translationStatus["status"], ttsStatus["status"], s3UploadStatus["status"])

        return ResponseEntity.status(status).body(
            mapOf(
                "translation" to translationStatus,
                "tts" to ttsStatus,
                "s3Upload" to s3UploadStatus,
                "overall" to if (allOk) "OK" else "DEGRADED"
            )
        )
    }

    /**
     * Returns current TTS provider and whether Google TTS is active.
     * Use to confirm NARRATION_GOOGLE_TTS process will run when stories are published.
     */
    /**
     * Manually trigger the narration pipeline for a curated story.
     * Use when pipeline stays PENDING (async job may not have started).
     */
    @PostMapping("/trigger-pipeline/{masterStoryId}")
    fun triggerPipeline(@PathVariable masterStoryId: Long): ResponseEntity<Map<String, Any>> {
        log.info("Dev trigger pipeline for masterStoryId={}", masterStoryId)
        storyProcessingService.processAsync(masterStoryId)
        return ResponseEntity.accepted().body(mapOf(
            "message" to "Pipeline triggered for story $masterStoryId (runs async)",
            "masterStoryId" to masterStoryId
        ))
    }

    @GetMapping("/tts-status")
    fun ttsStatus(): ResponseEntity<Map<String, Any>> {
        val provider = ttsProvider.trim().lowercase()
        val googleActive = provider == "google" && googleTtsApiKey.isNotBlank()
        return ResponseEntity.ok(mapOf(
            "provider" to provider,
            "googleActive" to googleActive,
            "keyConfigured" to googleTtsApiKey.isNotBlank(),
            "storyNarrationUses" to (if (googleActive) "Google Cloud TTS" else "Provider: $provider")
        ))
    }

    /**
     * Verify rewrite (conversational) status. Use when stories sound flat.
     * If limitExceeded=true or rewriteFailing, narration falls back to original text (flat read).
     */
    @GetMapping("/rewrite-status")
    fun rewriteStatus(): ResponseEntity<Map<String, Any>> {
        val dailyUsage = tokenUsageService.getDailyUsage()
        val limitExceeded = tokenUsageService.isLimitExceeded()
        val dailyLimit = appProperties.narration.dailyTokenLimit
        return ResponseEntity.ok(mapOf(
            "dailyTokenUsage" to dailyUsage,
            "dailyTokenLimit" to dailyLimit,
            "limitExceeded" to limitExceeded,
            "flatReadIfLimitExceeded" to "When limit exceeded, original text is used (flat/dry reading)",
            "suggestion" to when {
                limitExceeded -> "Raise NARRATION_DAILY_TOKEN_LIMIT or wait for midnight reset"
                dailyUsage > dailyLimit * 0.8 -> "Near limit; consider raising NARRATION_DAILY_TOKEN_LIMIT"
                else -> "OK - rewrite should produce conversational audio"
            }
        ))
    }

    /**
     * Verifies Google Cloud TTS connectivity.
     * Requires NARRATION_TTS_PROVIDER=google and GOOGLE_CLOUD_TTS_API_KEY set.
     * Synthesizes a short Tamil phrase to confirm the API key works.
     */
    @GetMapping("/verify-google-tts")
    fun verifyGoogleTts(): ResponseEntity<Map<String, Any>> {
        log.info("Verify Google TTS (provider={}, keyConfigured={})", ttsProvider, googleTtsApiKey.isNotBlank())
        val providerOk = ttsProvider.trim().equals("google", ignoreCase = true)
        if (!providerOk) {
            return ResponseEntity.ok(mapOf(
                "status" to "SKIPPED",
                "message" to "NARRATION_TTS_PROVIDER is '$ttsProvider', not 'google'. Set NARRATION_TTS_PROVIDER=google to use Google TTS.",
                "provider" to ttsProvider,
                "keyConfigured" to false
            ))
        }
        if (googleTtsApiKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf(
                "status" to "ERROR",
                "message" to "GOOGLE_CLOUD_TTS_API_KEY is not set. Add it to .env",
                "provider" to ttsProvider,
                "keyConfigured" to false
            ))
        }
        return try {
            // Direct API call to surface actual Google error (API key, quota, etc.)
            val requestBody = """{"input":{"text":"வணக்கம்"},"voice":{"languageCode":"ta-IN","name":"ta-IN-Wavenet-A"},"audioConfig":{"audioEncoding":"MP3","speakingRate":0.95}}"""
            val headers = HttpHeaders().apply {
                set("Content-Type", "application/json")
                set("X-Goog-Api-Key", googleTtsApiKey)
            }
            val entity = org.springframework.http.HttpEntity(requestBody, headers)
            val response = restTemplate.exchange(
                "https://texttospeech.googleapis.com/v1/text:synthesize",
                org.springframework.http.HttpMethod.POST,
                entity,
                Map::class.java
            )
            val body = response.body ?: throw IllegalStateException("Empty response")
            val audioContent = body["audioContent"] as? String ?: throw IllegalStateException("No audioContent in response")
            val bytes = java.util.Base64.getDecoder().decode(audioContent)
            if (bytes.isEmpty()) throw IllegalStateException("Empty audio")
            log.info("Google TTS OK: synthesized {} bytes (Tamil)", bytes.size)
            ResponseEntity.ok(mapOf(
                "status" to "OK",
                "message" to "Google Cloud TTS connected. Tamil synthesis successful.",
                "provider" to ttsProvider,
                "keyConfigured" to true,
                "bytes" to bytes.size
            ))
        } catch (e: HttpStatusCodeException) {
            val errorBody = e.responseBodyAsString.take(500)
            log.warn("Google TTS API error: {} body={}", e.statusCode, errorBody)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf(
                "status" to "ERROR",
                "message" to "Google API returned ${e.statusCode}: ${e.statusText}",
                "detail" to (errorBody.ifBlank { e.message } ?: "No details"),
                "provider" to ttsProvider,
                "keyConfigured" to true
            ))
        } catch (e: RestClientException) {
            log.warn("Google TTS request failed: {}", e.message)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf(
                "status" to "ERROR",
                "message" to (e.message ?: "Request failed"),
                "provider" to ttsProvider,
                "keyConfigured" to true
            ))
        } catch (e: Exception) {
            log.warn("Google TTS failed: {}", e.message)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf(
                "status" to "ERROR",
                "message" to (e.message ?: "Unknown error"),
                "provider" to ttsProvider,
                "keyConfigured" to true
            ))
        }
    }

    private fun verifyOpenAI(): Map<String, Any> {
        val apiKey = appProperties.openai.apiKey
        val baseUrl = appProperties.openai.baseUrl.trimEnd('/')
        if (apiKey.isBlank()) {
            return mapOf(
                "status" to "ERROR",
                "message" to "OPENAI_API_KEY not set",
                "configured" to false
            )
        }
        return try {
            val headers = HttpHeaders().apply {
                setBearerAuth(apiKey)
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = org.springframework.http.HttpEntity<Nothing>(headers)
            restTemplate.exchange(
                "$baseUrl/v1/models",
                org.springframework.http.HttpMethod.GET,
                entity,
                Map::class.java
            )
            mapOf(
                "status" to "OK",
                "message" to "OpenAI API reachable",
                "baseUrl" to baseUrl,
                "configured" to true
            )
        } catch (e: Exception) {
            mapOf(
                "status" to "ERROR",
                "message" to (e.message ?: "Unknown error"),
                "baseUrl" to baseUrl,
                "configured" to true
            )
        }
    }

    private fun verifyS3(): Map<String, Any> {
        val client = s3ClientProvider.getIfAvailable()
        if (client == null) {
            return mapOf(
                "status" to "ERROR",
                "message" to "S3Client not available (storage.type may not be s3)",
                "bucket" to appProperties.storage.s3Bucket
            )
        }
        val bucketOrArn = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }
        val region = appProperties.storage.s3Region.ifBlank { "us-east-1" }
        val isAccessPoint = bucketOrArn.startsWith("arn:aws:s3:")
        return try {
            if (isAccessPoint) {
                // Access Points don't support HeadBucket; use ListObjectsV2 with maxKeys=1 to verify
                client.listObjectsV2(
                    ListObjectsV2Request.builder()
                        .bucket(bucketOrArn)
                        .maxKeys(1)
                        .build()
                )
            } else {
                client.headBucket(HeadBucketRequest.builder().bucket(bucketOrArn).build())
            }
            mapOf(
                "status" to "OK",
                "message" to if (isAccessPoint) "S3 Access Point reachable" else "S3 bucket reachable",
                "bucket" to bucketOrArn,
                "region" to region,
                "accessPoint" to isAccessPoint
            )
        } catch (e: Exception) {
            mapOf(
                "status" to "ERROR",
                "message" to (e.message ?: "Unknown error"),
                "bucket" to bucketOrArn,
                "region" to region,
                "accessPoint" to isAccessPoint
            )
        }
    }
}
