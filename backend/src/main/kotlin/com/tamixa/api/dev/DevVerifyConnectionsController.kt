package com.tamixa.api.dev

import com.tamixa.api.ApiVersion
import com.tamixa.application.port.CoverVideoGenerationPort
import com.tamixa.application.narration.AudioStorageService
import com.tamixa.application.narration.StoryProcessingService
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.application.narration.TokenUsageService
import com.tamixa.application.translation.TranslationService
import com.tamixa.application.narration.TTSService
import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
import com.tamixa.application.port.voice.VoiceReferenceStoragePort
import com.tamixa.infrastructure.config.AppProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.core.task.TaskExecutor
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestTemplate
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import java.security.MessageDigest

/**
 * Dev-only endpoints to verify external service connections and the audio translation pipeline.
 * - GET /api/v1/dev/verify-connections – OpenAI, S3
 * - GET /api/v1/dev/verify-audio-translation – Translation (ta→en) → TTS → S3 upload
 * - GET /api/v1/dev/verify-google-tts – Google Cloud TTS connectivity (when NARRATION_TTS_PROVIDER=google)
 * - GET /api/v1/dev/tts-status – Current TTS provider and whether Google TTS is active for story processing
 * - GET /api/v1/dev/voice-cloning-status – ElevenLabs/XTTS config and bean presence for cloned voice flow
 * - POST /api/v1/dev/trigger-pipeline/{masterStoryId} – Manually trigger full pipeline (processSync, blocking in background)
 * - POST /api/v1/dev/trigger-retry/{masterStoryId} – Retry failed/stuck languages only (retryFailed)
 * - POST /api/v1/dev/recover-story/{masterStoryId} – Reset narration+retry state and rerun full pipeline
 * - GET /api/v1/dev/story-status/{id} – Story details and pipeline status (no auth)
 * - GET /api/v1/dev/story-audio-path/{id}?language=ta – resolved narration storage key for preview
 * - GET /api/v1/dev/story-audio-fetch/{id}?language=ta – stream test + diagnostics for preview audio
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
    private val storyLibraryService: StoryLibraryService,
    private val audioStorageProvider: ObjectProvider<AudioStorageService>,
    private val tokenUsageService: TokenUsageService,
    private val coverVideoGenerationProvider: ObjectProvider<CoverVideoGenerationPort>,
    @Value("\${app.narration.tts-provider:simulated}") private val ttsProvider: String,
    @Value("\${app.narration.google-tts-api-key:}") private val googleTtsApiKey: String,
    @Value("\${app.sora.enabled:false}") private val soraEnabled: Boolean,
    @Value("\${app.sora.model:sora-2}") private val soraModel: String,
    private val elevenLabsProvider: ObjectProvider<ElevenLabsVoiceCloningPort>,
    private val voiceReferenceStorageProvider: ObjectProvider<VoiceReferenceStoragePort>,
    @Qualifier("triggerPipelineExecutor") private val triggerPipelineExecutor: TaskExecutor
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/verify-connections")
    fun verifyConnections(): ResponseEntity<Map<String, Any>> {
        log.info("Verify connections request (OpenAI, S3)")
        val results = mutableMapOf<String, Map<String, Any>>()

        // OpenAI (general: models API)
        val openaiResult = verifyOpenAI()
        results["openai"] = openaiResult

        // Rewrite step: uses OpenAI Chat Completions (NarrationOpenAIAdapter). Pipeline REWRITING → this.
        results["openaiRewrite"] = mapOf(
            "usedBy" to "Pipeline rewrite step (REWRITING status). NarrationOpenAIAdapter → OpenAI /v1/chat/completions",
            "apiKeySet" to appProperties.openai.apiKey.isNotBlank(),
            "apiKeyHint" to (if (appProperties.openai.apiKey.isNotBlank()) "OPENAI_API_KEY is set" else "OPENAI_API_KEY not set. Add to .env: OPENAI_API_KEY=sk-..."),
            "readTimeoutMs" to appProperties.openai.readTimeoutMs,
            "readTimeoutEnv" to "OPENAI_READ_TIMEOUT_MS (default 60000). Set 45000 to fail faster if API is slow."
        )

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
            "note" to "Videos API may require separate Sora access. Trigger 'Regenerate cover' on a curated story to test."
        )

        val allOk = (openaiResult["status"] == "OK") &&
            (s3Result["status"] == "OK" || s3Result["status"] == "SKIPPED")
        val status = if (allOk) HttpStatus.OK else HttpStatus.SERVICE_UNAVAILABLE
        log.info("Verify connections complete openai={} s3={} soraEnabled={} overall={}", openaiResult["status"], s3Result["status"], soraEnabled, if (allOk) "OK" else "DEGRADED")

        val soraInfo = results["sora"] as Map<String, Any>
        val openaiRewriteInfo = results["openaiRewrite"] as Map<String, Any>
        return ResponseEntity
            .status(status)
            .body(
                mapOf<String, Any>(
                    "openai" to openaiResult,
                    "openaiRewrite" to openaiRewriteInfo,
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
    @GetMapping("/story-status/{id}")
    fun storyStatus(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val story = storyLibraryService.findById(id)
        if (story == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Story $id not found"))
        }
        val pipelineStatus = storyLibraryService.getPipelineStatusByStoryId(id)
        val translations = storyLibraryService.getTranslationsForStory(id)
        return ResponseEntity.ok(mapOf(
            "story" to mapOf(
                "id" to story.id,
                "title" to story.title,
                "status" to story.status,
                "language" to story.language,
                "narrationApprovedAt" to story.narrationApprovedAt?.toString(),
                "audioFileUrl" to (if (story.audioFileUrl.isNullOrBlank()) null else "${story.audioFileUrl.take(80)}...")
            ),
            "pipelineStatus" to pipelineStatus,
            "translations" to translations
        ))
    }

    @GetMapping("/story-audio-path/{id}")
    fun storyAudioPath(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String
    ): ResponseEntity<Map<String, Any?>> {
        val story = storyLibraryService.findById(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to "Story $id not found"))
        val normalized = com.tamixa.application.stream.StreamLanguageUtils.normalize(language)
        val path = storyLibraryService.getNarrationStoragePath(id, normalized)
        val s3Exists = if (path != null && path.startsWith("stories/")) {
            val client = s3ClientProvider.getIfAvailable()
            if (client == null) null else {
                try {
                    client.headObject(
                        HeadObjectRequest.builder()
                            .bucket(appProperties.storage.effectiveS3Bucket)
                            .key(path)
                            .build()
                    )
                    true
                } catch (_: Exception) {
                    false
                }
            }
        } else null
        return ResponseEntity.ok(
            mapOf(
                "storyId" to id,
                "requestedLanguage" to language,
                "normalizedLanguage" to normalized,
                "storyLanguage" to story.language,
                "storyAudioFileUrl" to story.audioFileUrl,
                "resolvedNarrationPath" to path,
                "ready" to (path != null),
                "s3ObjectExists" to s3Exists
            )
        )
    }

    @GetMapping("/story-audio-fetch/{id}")
    fun storyAudioFetch(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String
    ): ResponseEntity<Map<String, Any?>> {
        val normalized = com.tamixa.application.stream.StreamLanguageUtils.normalize(language)
        val key = storyLibraryService.getNarrationStoragePath(id, normalized)
        val client = s3ClientProvider.getIfAvailable()
            ?: return ResponseEntity.ok(
                mapOf(
                    "storyId" to id,
                    "requestedLanguage" to language,
                    "normalizedLanguage" to normalized,
                    "resolvedNarrationPath" to key,
                    "ok" to false,
                    "errorType" to "S3ClientUnavailable",
                    "error" to "S3 client is not available"
                )
            )
        if (key == null || !key.startsWith("stories/")) {
            return ResponseEntity.ok(
                mapOf(
                    "storyId" to id,
                    "requestedLanguage" to language,
                    "normalizedLanguage" to normalized,
                    "resolvedNarrationPath" to key,
                    "ok" to false,
                    "errorType" to "NoAudioPath",
                    "error" to "No narration storage path resolved"
                )
            )
        }
        return try {
            val bytes = client.getObjectAsBytes(
                GetObjectRequest.builder()
                    .bucket(appProperties.storage.effectiveS3Bucket)
                    .key(key)
                    .build()
            )
            ResponseEntity.ok(
                mapOf(
                    "storyId" to id,
                    "requestedLanguage" to language,
                    "normalizedLanguage" to normalized,
                    "resolvedNarrationPath" to key,
                    "bytes" to bytes.asByteArray().size,
                    "ok" to true
                )
            )
        } catch (e: Exception) {
            ResponseEntity.ok(
                mapOf(
                    "storyId" to id,
                    "requestedLanguage" to language,
                    "normalizedLanguage" to normalized,
                    "resolvedNarrationPath" to key,
                    "ok" to false,
                    "errorType" to e::class.simpleName,
                    "error" to (e.message ?: "unknown")
                )
            )
        }
    }

    @PostMapping("/trigger-pipeline/{masterStoryId}")
    fun triggerPipeline(@PathVariable masterStoryId: Long): ResponseEntity<Map<String, Any>> {
        log.info("PIPELINE trigger-pipeline masterStoryId={} (processSync running in background)", masterStoryId)
        triggerPipelineExecutor.execute { storyProcessingService.processSync(masterStoryId) }
        return ResponseEntity.accepted().body(mapOf(
            "message" to "Pipeline triggered for story $masterStoryId (processSync running in background)",
            "masterStoryId" to masterStoryId
        ))
    }

    @PostMapping("/trigger-retry/{masterStoryId}")
    fun triggerRetry(@PathVariable masterStoryId: Long): ResponseEntity<Map<String, Any>> {
        log.info("PIPELINE trigger-retry masterStoryId={} (retryFailed running in background)", masterStoryId)
        triggerPipelineExecutor.execute { storyProcessingService.retryFailed(masterStoryId) }
        return ResponseEntity.accepted().body(mapOf(
            "message" to "Retry triggered for story $masterStoryId (retryFailed running in background)",
            "masterStoryId" to masterStoryId
        ))
    }

    /**
     * Full story recovery for stale statuses:
     * - clears narration rows for this story
     * - sets translations to PENDING
     * - resets retryCount to 0 and clears lastError
     * - clears master audio URL
     * Then reruns full pipeline in background.
     */
    @PostMapping("/recover-story/{masterStoryId}")
    fun recoverStory(@PathVariable masterStoryId: Long): ResponseEntity<Map<String, Any>> {
        log.info("PIPELINE recover-story masterStoryId={}", masterStoryId)
        val resetCount = storyLibraryService.resetNarrationAndRetryStateForStory(masterStoryId)
        triggerPipelineExecutor.execute { storyProcessingService.republishLanguages(masterStoryId, emptyList()) }
        return ResponseEntity.accepted().body(mapOf(
            "message" to "Recovered story $masterStoryId and triggered full republish pipeline",
            "masterStoryId" to masterStoryId,
            "translationsReset" to resetCount
        ))
    }

    /**
     * Reset stuck translations (REWRITING, TRANSLATING, TTS_PROCESSING) to PENDING, then trigger retry.
     * Use when pipeline hangs and translations stay in intermediate states.
     */
    @PostMapping("/reset-stuck-and-retry/{masterStoryId}")
    fun resetStuckAndRetry(@PathVariable masterStoryId: Long): ResponseEntity<Map<String, Any>> {
        log.info("PIPELINE reset-stuck-and-retry masterStoryId={}", masterStoryId)
        val reset = storyLibraryService.resetStuckTranslationsToPending(masterStoryId)
        triggerPipelineExecutor.execute { storyProcessingService.retryFailed(masterStoryId) }
        return ResponseEntity.accepted().body(mapOf(
            "message" to "Reset $reset stuck translation(s) to PENDING, retry triggered",
            "masterStoryId" to masterStoryId,
            "resetCount" to reset
        ))
    }

    /**
     * Normalizes master story status consistency across all curated stories.
     * Rule: narrationApprovedAt != null => status must be PUBLISHED.
     */
    @GetMapping("/normalize-story-statuses")
    fun normalizeStoryStatuses(): ResponseEntity<Map<String, Any>> {
        val report = storyLibraryService.normalizeMasterStatusesForApprovedStories()
        return ResponseEntity.ok(
            mapOf(
                "message" to "Story status normalization completed",
                "rule" to "narrationApprovedAt != null => status=PUBLISHED",
                "result" to report
            )
        )
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
     * Voice cloning (ElevenLabs + XTTS) status for cloned-voice narration flow.
     * Use to verify ELEVENLABS_API_KEY, VOICE_CLONING_ENABLED, S3 reference storage, and XTTS base URL.
     */
    @GetMapping("/voice-cloning-status")
    fun voiceCloningStatus(): ResponseEntity<Map<String, Any>> {
        val vc = appProperties.voiceCloning
        val elevenLabsBean = elevenLabsProvider.getIfAvailable()
        val refStorageBean = voiceReferenceStorageProvider.getIfAvailable()
        val elevenLabsReady = vc.enabled && vc.elevenLabsApiKey.isNotBlank() && elevenLabsBean != null
        val refStorageReady = refStorageBean != null
        val xttsConfigured = vc.xttsBaseUrl.isNotBlank()
        return ResponseEntity.ok(mapOf(
            "voiceCloningEnabled" to vc.enabled,
            "elevenLabsApiKeySet" to vc.elevenLabsApiKey.isNotBlank(),
            "elevenLabsBeanPresent" to (elevenLabsBean != null),
            "elevenLabsReady" to elevenLabsReady,
            "voiceReferenceStoragePresent" to refStorageReady,
            "xttsBaseUrlSet" to xttsConfigured,
            "flow" to mapOf(
                "1_existing_voice_id" to "If profile has elevenlabs_voice_id → ElevenLabs synthesize",
                "2_create_from_reference" to "If profile has reference_audio_path + ElevenLabs + S3 ref storage → addVoice then synthesize",
                "3_xtts_fallback" to "If no ElevenLabs or addVoice failed → XTTS (when xtts-base-url set)"
            ),
            "checklist" to listOf(
                "Set VOICE_CLONING_ENABLED=true and ELEVENLABS_API_KEY in .env",
                "Use S3 (app.storage.type=s3) so reference audio can be read for addVoice",
                "Optional: set XTTS_BASE_URL for fallback when ElevenLabs fails or is disabled"
            )
        ))
    }

    /**
     * Deep ElevenLabs diagnostic for cloned voice flow.
     * Returns only masked/fingerprint key metadata (never raw secrets), plus in-process synth probe.
     */
    @GetMapping("/verify-elevenlabs")
    fun verifyElevenLabs(
        @RequestParam voiceId: String,
        @RequestParam(defaultValue = "ta") language: String
    ): ResponseEntity<Map<String, Any?>> {
        val vc = appProperties.voiceCloning
        val configuredKey = vc.elevenLabsApiKey
        val envKey = System.getenv("ELEVENLABS_API_KEY") ?: ""
        val provider = vc.provider.trim().lowercase()
        val bean = elevenLabsProvider.getIfAvailable()

        val userStatus = if (configuredKey.isBlank()) {
            null
        } else {
            try {
                val headers = HttpHeaders().apply { set("xi-api-key", configuredKey.trim()) }
                val entity = org.springframework.http.HttpEntity<Any>(headers)
                restTemplate.exchange(
                    "${vc.elevenLabsBaseUrl.trimEnd('/')}/v1/user",
                    org.springframework.http.HttpMethod.GET,
                    entity,
                    Map::class.java
                )
                200
            } catch (e: HttpStatusCodeException) {
                e.statusCode.value()
            } catch (_: Exception) {
                -1
            }
        }

        val synth = if (bean == null) null else try {
            val bytes = bean.synthesize("Vanakkam, this is a runtime check.", voiceId.trim(), language.trim())
            if (bytes != null && bytes.isNotEmpty()) mapOf("ok" to true, "bytes" to bytes.size)
            else mapOf("ok" to false, "bytes" to 0)
        } catch (e: Exception) {
            mapOf("ok" to false, "error" to (e.message ?: "unknown"))
        }

        return ResponseEntity.ok(
            mapOf(
                "provider" to provider,
                "voiceCloningEnabled" to vc.enabled,
                "elevenLabsBeanPresent" to (bean != null),
                "configuredKey" to mapOf(
                    "present" to configuredKey.isNotBlank(),
                    "length" to configuredKey.length,
                    "last4" to configuredKey.takeLast(4),
                    "fingerprint" to secretFingerprint(configuredKey)
                ),
                "envKey" to mapOf(
                    "present" to envKey.isNotBlank(),
                    "length" to envKey.length,
                    "last4" to envKey.takeLast(4),
                    "fingerprint" to secretFingerprint(envKey)
                ),
                "keysMatchByFingerprint" to (
                    configuredKey.isNotBlank() &&
                        envKey.isNotBlank() &&
                        secretFingerprint(configuredKey) == secretFingerprint(envKey)
                    ),
                "userApiStatus" to userStatus,
                "synthesizeProbe" to synth
            )
        )
    }

    private fun secretFingerprint(secret: String): String {
        if (secret.isBlank()) return ""
        val bytes = MessageDigest.getInstance("SHA-256").digest(secret.trim().toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(12)
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
        val bucketOrArn = appProperties.storage.effectiveS3Bucket
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
