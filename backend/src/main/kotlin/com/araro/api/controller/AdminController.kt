package com.araro.api.controller

import com.araro.api.ApiVersion
import com.araro.api.admin.CuratedStoryMapper.toResponse
import com.araro.api.admin.dto.BulkDeleteRequest
import com.araro.api.admin.dto.BulkPublishRequest
import com.araro.api.admin.dto.BulkUpdateCategoryRequest
import com.araro.api.admin.dto.RepublishRequest
import com.araro.api.admin.dto.CreateCuratedStoryRequest
import com.araro.api.admin.dto.SuggestRephraseRequest
import com.araro.api.admin.dto.AdminInvoiceDto
import com.araro.api.admin.dto.FlagStoryRequest
import com.araro.api.admin.dto.StoryDetailDto
import com.araro.api.admin.dto.PagedResponse
import com.araro.api.admin.dto.RevenueRowDto
import com.araro.api.admin.dto.SubscriptionMetricsDto
import com.araro.application.analytics.CompletionMetricsDto
import com.araro.application.analytics.RetentionMetricsDto
import com.araro.application.admin.AdminParentNotFoundException
import com.araro.application.admin.AdminService
import com.araro.application.admin.InvoiceNotRefundableException
import com.araro.application.admin.InvoiceNotFoundException
import com.araro.application.admin.StoryNotFoundException
import com.araro.application.curated.CuratedStoryIllustrationService
import com.araro.application.curated.CuratedStoryRephraseService
import com.araro.application.curated.CuratedStoryService
import com.araro.application.narration.StoryProcessingService
import com.araro.application.port.StoryRepositoryPort
import com.araro.application.stream.AudioStreamService
import com.araro.application.stream.CoverImageUrlResolver
import com.araro.domain.StoryStatus
import com.araro.infrastructure.config.AppProperties
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

@RestController
@RequestMapping("${ApiVersion.V1}/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','REVENUE_ANALYST','CONTENT_MANAGER','SUPPORT')")
class AdminController(
    private val adminService: AdminService,
    private val curatedStoryService: CuratedStoryService,
    private val storyProcessingService: StoryProcessingService,
    @Qualifier("triggerPipelineExecutor") private val triggerPipelineExecutor: TaskExecutor,
    private val storyRepository: StoryRepositoryPort,
    private val curatedStoryIllustrationService: CuratedStoryIllustrationService,
    private val curatedStoryRephraseService: CuratedStoryRephraseService,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    private val audioStreamService: AudioStreamService,
    private val appProperties: AppProperties,
    @Autowired(required = false) private val s3Client: S3Client?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/analytics/retention")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun getRetention(@RequestParam(defaultValue = "30") days: Int): ResponseEntity<RetentionMetricsDto> =
        ResponseEntity.ok(adminService.getRetentionMetrics(days))

    @GetMapping("/analytics/completion")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun getCompletion(@RequestParam(defaultValue = "30") days: Int): ResponseEntity<CompletionMetricsDto> =
        ResponseEntity.ok(adminService.getCompletionMetrics(days))

    @GetMapping("/info")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun info(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("message" to "Admin only area"))
    }

    @GetMapping("/parents")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun getParents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) status: String?
    ) = ResponseEntity.ok(adminService.getParents(page, size, email, status))

    @GetMapping("/children")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_CHILDREN')")
    fun getChildren(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) parentId: Long?
    ) = ResponseEntity.ok(adminService.getChildren(page, size, parentId))

    @GetMapping("/stories/{id}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_STORIES')")
    fun getStory(@PathVariable id: Long): ResponseEntity<StoryDetailDto> {
        val dto = adminService.getStoryById(id)
        return if (dto != null) ResponseEntity.ok(dto)
        else ResponseEntity.notFound().build()
    }

    @GetMapping("/stories")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_STORIES')")
    fun getStories(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) theme: String?
    ): ResponseEntity<*> {
        val storyStatus = status?.let { runCatching { StoryStatus.valueOf(it.uppercase()) }.getOrNull() }
        return ResponseEntity.ok(adminService.getStories(page, size, storyStatus, theme))
    }

    @GetMapping("/voice-logs")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_VOICE_LOGS')")
    fun getVoiceUploadLogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = ResponseEntity.ok(adminService.getVoiceUploadLogs(page, size))

    @GetMapping("/subscriptions")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_SUBSCRIPTIONS')")
    fun getSubscriptionStatus(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = ResponseEntity.ok(adminService.getSubscriptionStatus(page, size))

    @GetMapping("/health")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_HEALTH')")
    fun getHealth() = ResponseEntity.ok(adminService.getHealth())

    @GetMapping("/metrics/ai")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_AI_METRICS')")
    fun getAiMetrics() = ResponseEntity.ok(adminService.getAiMetrics())

    @GetMapping("/metrics/revenue")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun getRevenueMetrics(@RequestParam(required = false) month: String?) =
        ResponseEntity.ok(
            adminService.getRevenueMetrics(
                month?.let { runCatching { java.time.YearMonth.parse(it) }.getOrNull() }
            )
        )

    @GetMapping("/metrics/subscription")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun getSubscriptionMetrics() =
        ResponseEntity.ok(adminService.getSubscriptionMetrics())

    @GetMapping("/metrics/revenue/table")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun getRevenueTable(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) plan: String?
    ): ResponseEntity<PagedResponse<RevenueRowDto>> =
        ResponseEntity.ok(adminService.getRevenueTable(page, size, search, plan))

    @GetMapping("/kafka-events")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_KAFKA')")
    fun getKafkaEvents(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = ResponseEntity.ok(adminService.getKafkaEvents(page, size))

    @GetMapping("/audit-trail")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_AUDIT')")
    fun getAuditTrail(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = ResponseEntity.ok(adminService.getAuditTrail(page, size))

    @GetMapping("/curated-stories")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun getCuratedStories(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<PagedResponse<com.araro.api.admin.dto.CuratedStoryResponse>> {
        val pageResult = curatedStoryService.findAllByStatus(status, page, size)
        return ResponseEntity.ok(
            PagedResponse(
                content = pageResult.content.map { story ->
                    val resolvedAudio = story.audioFileUrl?.takeIf { it.isNotBlank() }
                        ?: curatedStoryService.resolveAudioFileUrlForDisplay(story.id, story.language)
                    story.toResponse(
                        coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                        coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
                    ).copy(audioFileUrl = resolvedAudio)
                },
                page = pageResult.number,
                size = pageResult.size,
                totalElements = pageResult.totalElements,
                totalPages = pageResult.totalPages,
                first = pageResult.isFirst,
                last = pageResult.isLast
            )
        )
    }

    @GetMapping("/stories/{id}/status")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun getStoryStatus(@PathVariable id: Long): ResponseEntity<Map<String, String>> =
        getCuratedStoryPipelineStatus(id)

    @GetMapping("/curated-stories/{id}")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun getCuratedStory(@PathVariable id: Long): ResponseEntity<com.araro.api.admin.dto.CuratedStoryResponse> {
        val story = curatedStoryService.findById(id) ?: return ResponseEntity.notFound().build()
        // Use findByIdAndLanguage to get narrated content in Story text field (matches audio)
        val response = curatedStoryService.findByIdAndLanguage(id, story.language.ifBlank { "ta" })
        return if (response != null) ResponseEntity.ok(response)
        else ResponseEntity.ok(
            story.toResponse(
                coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
            )
        )
    }

    @PostMapping("/curated-stories/{id}/generate-cover")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun generateCuratedStoryCover(
        @PathVariable id: Long,
        @org.springframework.web.bind.annotation.RequestParam(name = "force", required = false, defaultValue = "false") force: Boolean
    ): ResponseEntity<*> {
        log.info("Generate cover requested for curated story id={} force={}", id, force)
        val story = curatedStoryService.findById(id) ?: return ResponseEntity.notFound().build<Unit>()
        val updated = curatedStoryIllustrationService.generateCoverForStory(story, force)
        return if (updated != null) {
            log.info("Generate cover success for curated story id={}", id)
            ResponseEntity.ok(
                updated.toResponse(
                    coverImageUrlResolver.resolveCoverPath(updated.coverImageUrl),
                    coverImageUrlResolver.resolveCoverVideoPath(updated.coverVideoUrl)
                )
            )
        } else {
            log.warn("Generate cover failed for curated story id={} (DALL-E returned null or S3 storage failed)", id)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("message" to "Cover generation failed (DALL-E or storage unavailable)"))
        }
    }

    @PostMapping("/curated-stories/{id}/suggest-rephrase")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun suggestRephrase(
        @PathVariable id: Long,
        @Valid @RequestBody request: SuggestRephraseRequest
    ): ResponseEntity<*> {
        val story = curatedStoryService.findById(id) ?: return ResponseEntity.notFound().build<Unit>()
        val title = request.title?.takeIf { it.isNotBlank() } ?: story.title
        val content = request.content?.takeIf { it.isNotBlank() } ?: story.content
        if (content.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Content is required"))
        }
        val suggestion = curatedStoryRephraseService.suggestRephrase(title, content)
        return if (suggestion != null) ResponseEntity.ok(mapOf(
            "suggestedTitle" to (suggestion.suggestedTitle ?: ""),
            "suggestedContent" to suggestion.suggestedContent
        ))
        else ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(mapOf("message" to "Rephrase suggestion failed (OpenAI unavailable)"))
    }

    @GetMapping("/curated-stories/{id}/pipeline-status")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun getCuratedStoryPipelineStatus(
        @PathVariable id: Long
    ): ResponseEntity<Map<String, String>> {
        val status = curatedStoryService.getPipelineStatusByStoryId(id)
        return if (status.isEmpty()) ResponseEntity.notFound().build()
        else ResponseEntity.ok(status)
    }

    /**
     * Returns a playable stream URL for admin preview of narration audio.
     * Used to verify translation quality and conversational tone (vs flat reading).
     */
    @GetMapping("/curated-stories/{id}/stream-url")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun getCuratedStoryStreamUrl(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String
    ): ResponseEntity<Map<String, String>> {
        val url = audioStreamService.getCuratedStreamUrl(id, language, null)
        return if (url != null) ResponseEntity.ok(mapOf("streamUrl" to url))
        else ResponseEntity.notFound().build()
    }

    /**
     * Streams narration audio bytes for admin preview.
     * Use this URL for Audio playback so it works regardless of AUDIO_PUBLIC_BASE_URL
     * (e.g. when set for Android emulator 10.0.2.2, browser cannot reach it).
     */
    @GetMapping("/curated-stories/{id}/preview-audio")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun streamCuratedStoryAudio(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String
    ): ResponseEntity<Any> {
        val key = curatedStoryService.getNarrationStoragePath(id, language)
        if (key == null) {
            log.warn("Admin stream: no audio for story {} lang={}", id, language)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Audio not ready for this language yet. Check pipeline status."))
        }
        val client = s3Client ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("message" to "Audio storage not configured (S3 required)"))
        val bucket = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }
        return try {
            val response = client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())
            val bytes = response.readAllBytes()
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .body(bytes)
        } catch (e: NoSuchKeyException) {
            log.warn("Admin stream: audio not found in S3 key={}", key)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Audio file missing in storage"))
        } catch (e: Exception) {
            log.warn("Admin stream failed storyId={} lang={}: {}", id, language, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to (e.message ?: "Storage error")))
        }
    }

    /**
     * Streams TTS narration audio for admin preview of user-generated stories.
     * Uses story.audioFileUrl (conversational narration from GeneratedStoryNarrationService).
     */
    @GetMapping("/stories/{id}/preview-audio")
    @PreAuthorize("@adminAuth.hasPermission('MODERATE_STORIES')")
    fun streamGeneratedStoryAudio(@PathVariable id: Long): ResponseEntity<Any> {
        val story = storyRepository.findById(id) ?: run {
            log.warn("Admin stream: generated story id={} not found", id)
            return ResponseEntity.notFound().build()
        }
        if (story.status != StoryStatus.READY) {
            log.warn("Admin stream: generated story id={} not READY (status={})", id, story.status)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Audio not ready. Story must be READY."))
        }
        val key = story.audioFileUrl?.takeIf { it.isNotBlank() && it.startsWith("stories/") }
            ?: run {
                log.warn("Admin stream: generated story id={} has no audio path", id)
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapOf("message" to "Audio not ready for this story yet."))
            }
        val client = s3Client ?: return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("message" to "Audio storage not configured (S3 required)"))
        val bucket = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }
        return try {
            val response = client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())
            val bytes = response.readAllBytes()
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .body(bytes)
        } catch (e: NoSuchKeyException) {
            log.warn("Admin stream: generated story audio not found in S3 key={}", key)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Audio file missing in storage"))
        } catch (e: Exception) {
            log.warn("Admin stream failed generated storyId={}: {}", id, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to (e.message ?: "Storage error")))
        }
    }

    @PutMapping("/curated-stories/{id}")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun updateCuratedStory(
        @PathVariable id: Long,
        @Valid @RequestBody request: com.araro.api.admin.dto.CreateCuratedStoryRequest
    ): ResponseEntity<*> {
        log.info("Admin update curated story id={} status={} regenerateNarration={}", id, request.status, request.regenerateNarration)
        val shouldRegenerate = request.regenerateNarration != false
        val story = curatedStoryService.update(
            id = id,
            title = request.title,
            content = request.content,
            theme = request.theme,
            language = request.language.ifBlank { "ta" },
            age = request.age,
            childName = request.childName.ifBlank { "Child" },
            moral = request.moral,
            status = request.status.ifBlank { "DRAFT" },
            coverImageUrl = request.coverImageUrl?.takeIf { it.isNotBlank() }?.let { coverImageUrlResolver.normalizeForStorage(it) ?: it },
            coverVideoUrl = request.coverVideoUrl?.takeIf { it.isNotBlank() }?.let { coverImageUrlResolver.normalizeForStorage(it) ?: it },
            emotionMode = request.emotionMode,
            updateNarratedOnly = request.status == "PUBLISHED" && !shouldRegenerate
        )
        if (story != null) {
            if (story.status == "PUBLISHED" && shouldRegenerate) {
                log.info("Admin curated story id={} is PUBLISHED, triggering invalidate & reprocess in background", id)
                Thread {
                    log.info(">>> PIPELINE TASK STARTED (Update & publish) storyId={} thread={}", id, Thread.currentThread().name)
                    try {
                        storyProcessingService.invalidateAndReprocess(id)
                        log.info(">>> PIPELINE TASK COMPLETED (Update & publish) storyId={}", id)
                    } catch (e: Exception) {
                        log.error(">>> PIPELINE TASK FAILED (Update & publish) storyId={} error={}", id, e.message, e)
                    }
                }.apply { name = "invalidate-reprocess-$id"; isDaemon = false; start() }
            } else if (story.status == "PUBLISHED" && !shouldRegenerate) {
                log.info("Admin curated story id={} update narrated content only (regenerateNarration=false), no pipeline", id)
            }
            return ResponseEntity.ok(
                story.toResponse(
                    coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                    coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
                )
            )
        }
        log.warn("Admin update curated story id={} not found", id)
        return ResponseEntity.notFound().build<com.araro.api.admin.dto.CuratedStoryResponse>()
    }

    @PostMapping("/stories/{id}/retry")
    @PreAuthorize("@adminAuth.hasPermission('MODERATE_STORIES')")
    fun retryStory(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        triggerPipelineExecutor.execute {
            try {
                storyProcessingService.retryFailed(id)
                log.info("Retry pipeline completed for story id={}", id)
            } catch (e: Exception) {
                log.error("Retry pipeline failed for story id={}: {}", id, e.message, e)
            }
        }
        return ResponseEntity.ok(mapOf("message" to "Retry triggered for story $id"))
    }

    @PostMapping("/curated-stories/{id}/retry")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun retryCuratedStory(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        triggerPipelineExecutor.execute {
            try {
                storyProcessingService.retryFailed(id)
                log.info("Retry pipeline completed for story id={}", id)
            } catch (e: Exception) {
                log.error("Retry pipeline failed for story id={}: {}", id, e.message, e)
            }
        }
        return ResponseEntity.ok(mapOf("message" to "Retry triggered for curated story $id"))
    }

    /**
     * Trigger pipeline when stuck at PENDING (e.g. async never started).
     * Always runs processSync in background (bypasses @Async) so pipeline reliably starts.
     */
    @PostMapping("/curated-stories/{id}/trigger-pipeline")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun triggerCuratedStoryPipeline(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        log.info("Trigger pipeline for story id={} (background)", id)
        triggerPipelineExecutor.execute {
            try {
                storyProcessingService.processSync(id)
                log.info("Trigger pipeline completed for story id={}", id)
            } catch (e: Exception) {
                log.error("Trigger pipeline failed for story id={}: {}", id, e.message, e)
            }
        }
        return ResponseEntity.accepted()
            .body(mapOf("message" to "Pipeline triggered for story $id (running in background, ~5 min)"))
    }

    /**
     * Remove legacy audio (story_audio table + curated_stories.audio_file_url). Forces use of
     * story_narration_audio only (Chirp3-HD pipeline). Use after migrating to new TTS.
     */
    @DeleteMapping("/curated-stories/legacy-audio")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun clearLegacyAudio(): ResponseEntity<Map<String, Any>> {
        val (deletedAudio, clearedUrls, s3ObjectsDeleted) = curatedStoryService.clearLegacyAudio()
        return ResponseEntity.ok(
            mapOf(
                "message" to "Legacy audio removed",
                "storyAudioDeleted" to deletedAudio,
                "curatedAudioUrlsCleared" to clearedUrls,
                "s3ObjectsDeleted" to s3ObjectsDeleted
            )
        )
    }

    /**
     * Regenerate narration audio for all languages. Clears existing audio and reprocesses
     * (rewrite + TTS). Use when audio sounds flat to get conversational narration.
     */
    @PostMapping("/curated-stories/{id}/regenerate-narration")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun regenerateCuratedStoryNarration(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        triggerPipelineExecutor.execute {
            try {
                storyProcessingService.regenerateNarration(id)
                log.info("Regenerate pipeline completed for story id={}", id)
            } catch (e: Exception) {
                log.error("Regenerate pipeline failed for story id={}: {}", id, e.message, e)
            }
        }
        return ResponseEntity.ok(mapOf("message" to "Regenerate narration triggered for curated story $id"))
    }

    /**
     * Republish: clear audio for selected languages and reprocess. Empty languages = all.
     */
    @PostMapping("/curated-stories/{id}/republish")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun republishCuratedStory(
        @PathVariable id: Long,
        @RequestBody(required = false) request: RepublishRequest?
    ): ResponseEntity<Map<String, String>> {
        val languages = request?.languages?.filter { it.isNotBlank() } ?: emptyList()
        log.info("Republish requested storyId={} languages={}", id, languages)
        Thread {
            log.info(">>> PIPELINE TASK STARTED (Update & republish) storyId={} thread={}", id, Thread.currentThread().name)
            try {
                storyProcessingService.republishLanguages(id, languages)
                log.info(">>> PIPELINE TASK COMPLETED (Update & republish) storyId={}", id)
            } catch (e: Exception) {
                log.error(">>> PIPELINE TASK FAILED (Update & republish) storyId={} error={}", id, e.message, e)
            }
        }.apply { name = "republish-$id"; isDaemon = false; start() }
        val scope = if (languages.isEmpty()) "all languages" else "languages ${languages.joinToString()}"
        return ResponseEntity.ok(mapOf("message" to "Republish triggered for curated story $id ($scope)"))
    }

    @PutMapping("/curated-stories/bulk-publish")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun bulkPublish(@Valid @RequestBody request: BulkPublishRequest): ResponseEntity<Map<String, Any>> {
        val count = curatedStoryService.bulkPublish(request.ids)
        request.ids.forEach { id ->
            triggerPipelineExecutor.execute {
                try {
                    storyProcessingService.processSync(id)
                    log.info("Bulk publish pipeline completed for story id={}", id)
                } catch (e: Exception) {
                    log.error("Bulk publish pipeline failed for story id={}: {}", id, e.message, e)
                }
            }
        }
        return ResponseEntity.ok(mapOf("updated" to count, "ids" to request.ids))
    }

    @PutMapping("/curated-stories/bulk-category")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun bulkUpdateCategory(@Valid @RequestBody request: BulkUpdateCategoryRequest): ResponseEntity<Map<String, Any>> {
        val count = curatedStoryService.bulkUpdateCategory(request.ids, request.theme)
        return ResponseEntity.ok(mapOf("updated" to count, "theme" to request.theme))
    }

    @DeleteMapping("/curated-stories/{id}")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun deleteCuratedStory(@PathVariable id: Long): ResponseEntity<*> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        return if (curatedStoryService.deleteById(id)) {
            adminService.recordAdminAuditAction(adminEmail, "delete_curated_story", "curated_story", id.toString(), "Curated story deleted")
            ResponseEntity.noContent().build<Unit>()
        } else {
            ResponseEntity.notFound().build<Unit>()
        }
    }

    @DeleteMapping("/curated-stories/bulk")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun bulkDeleteCuratedStories(@Valid @RequestBody request: BulkDeleteRequest): ResponseEntity<Map<String, Any>> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        val deleted = curatedStoryService.deleteByIds(request.ids)
        if (deleted > 0) {
            adminService.recordAdminAuditAction(adminEmail, "bulk_delete_curated_stories", "curated_story", null, "Deleted ${request.ids.size} stories: ${request.ids}")
        }
        return ResponseEntity.ok(mapOf("deleted" to deleted, "ids" to request.ids))
    }

    @PostMapping("/stories")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun createStory(@Valid @RequestBody request: CreateCuratedStoryRequest): ResponseEntity<*> =
        createCuratedStory(request)

    @PostMapping("/curated-stories")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_CURATED_STORIES')")
    fun createCuratedStory(@Valid @RequestBody request: CreateCuratedStoryRequest): ResponseEntity<*> {
        val story = curatedStoryService.create(
            title = request.title,
            content = request.content,
            theme = request.theme,
            language = request.language.ifBlank { "ta" },
            age = request.age,
            childName = request.childName.ifBlank { "Child" },
            moral = request.moral,
            audioFileUrl = request.audioFileUrl,
            status = request.status.ifBlank { "DRAFT" },
            coverImageUrl = coverImageUrlResolver.normalizeForStorage(request.coverImageUrl) ?: request.coverImageUrl,
            emotionMode = request.emotionMode
        )
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(
            story.toResponse(
                coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
            )
        )
    }

    @PostMapping("/stories/{id}/flag")
    @PreAuthorize("@adminAuth.hasPermission('MODERATE_STORIES')")
    fun flagStory(
        @PathVariable id: Long,
        @Valid @RequestBody request: FlagStoryRequest?
    ): ResponseEntity<Unit> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return try {
            adminService.flagStory(adminEmail, id, request?.reason)
            ResponseEntity.ok().build()
        } catch (e: StoryNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/stories/{id}/approve")
    @PreAuthorize("@adminAuth.hasPermission('MODERATE_STORIES')")
    fun approveStory(@PathVariable id: Long): ResponseEntity<Unit> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return try {
            adminService.approveStory(adminEmail, id)
            ResponseEntity.ok().build()
        } catch (e: StoryNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/stories/{id}/reject")
    @PreAuthorize("@adminAuth.hasPermission('MODERATE_STORIES')")
    fun rejectStory(@PathVariable id: Long): ResponseEntity<Unit> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return try {
            adminService.rejectStory(adminEmail, id)
            ResponseEntity.ok().build()
        } catch (e: StoryNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/parents/{id}/suspend")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun suspendParent(@PathVariable id: Long): ResponseEntity<Unit> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return try {
            adminService.suspendParent(adminEmail, id)
            ResponseEntity.ok().build()
        } catch (e: AdminParentNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @PostMapping("/parents/{id}/unsuspend")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun unsuspendParent(@PathVariable id: Long): ResponseEntity<Unit> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return try {
            adminService.unsuspendParent(adminEmail, id)
            ResponseEntity.ok().build()
        } catch (e: AdminParentNotFoundException) {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/metrics/story-usage")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_AI_METRICS')")
    fun getStoryUsagePerDay(@RequestParam(defaultValue = "7") days: Int) =
        ResponseEntity.ok(adminService.getStoryUsagePerDay(days.coerceIn(1, 90)))

    @GetMapping("/invoices")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_INVOICES')")
    fun getInvoices(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<AdminInvoiceDto>> =
        ResponseEntity.ok(adminService.getInvoices(page, size))

    @PostMapping("/invoices/{id}/refund")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_INVOICES')")
    fun refundInvoice(@PathVariable id: Long): ResponseEntity<Unit> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        adminService.refundInvoice(adminEmail, id)
        return ResponseEntity.ok().build()
    }
}
