package com.tamixa.api.controller

import com.tamixa.api.ApiVersion
import com.tamixa.api.admin.LibraryStoryMapper.toResponse
import com.tamixa.api.admin.dto.BulkDeleteRequest
import com.tamixa.api.admin.dto.BulkGenerateStoriesRequest
import com.tamixa.api.admin.dto.BulkPublishRequest
import com.tamixa.api.admin.dto.BulkUpdateCategoryRequest
import com.tamixa.api.admin.dto.PatchVoiceProfileRequest
import com.tamixa.api.admin.dto.ReviewNotesRequest
import com.tamixa.api.admin.dto.CreateReferralCodeRequest
import com.tamixa.api.admin.dto.CreateShortContentRequest
import com.tamixa.api.admin.dto.GenerateShortContentRequest
import com.tamixa.api.admin.dto.GenerateShortContentResponse
import com.tamixa.api.admin.dto.ReferralCodeDto
import com.tamixa.api.admin.dto.ShortContentDto
import com.tamixa.api.admin.dto.UpdateShortContentRequest
import com.tamixa.api.admin.dto.RegenerateNarrationRequest
import com.tamixa.api.admin.dto.RepublishRequest
import com.tamixa.api.admin.dto.TtsPreviewRequest
import com.tamixa.api.admin.dto.UpdateReferralCodeRequest
import com.tamixa.api.admin.dto.CreateParentRequest
import com.tamixa.api.admin.dto.CreateLibraryStoryRequest
import com.tamixa.api.admin.dto.SuggestRephraseRequest
import com.tamixa.api.admin.dto.UpdateParentRequest
import com.tamixa.api.admin.dto.UpdateTranslationRequest
import com.tamixa.api.admin.dto.AdminInvoiceDto
import com.tamixa.api.admin.dto.FlagStoryRequest
import com.tamixa.api.admin.dto.StoryDetailDto
import com.tamixa.api.admin.dto.ParentDetailDto
import com.tamixa.api.admin.dto.PagedResponse
import com.tamixa.api.admin.dto.RevenueRowDto
import com.tamixa.api.admin.dto.SubscriptionMetricsDto
import com.tamixa.application.analytics.CompletionMetricsDto
import com.tamixa.application.analytics.RetentionMetricsDto
import com.tamixa.application.analytics.VoiceCloneAnalyticsService
import com.tamixa.application.admin.AdminParentNotFoundException
import com.tamixa.application.admin.AdminService
import com.tamixa.application.admin.ProcessingJobService
import com.tamixa.application.admin.ParentHasDependentsException
import com.tamixa.application.auth.EmailAlreadyExistsException
import com.tamixa.application.auth.PhoneAlreadyInUseException
import com.tamixa.application.admin.InvoiceNotRefundableException
import com.tamixa.application.admin.InvoiceNotFoundException
import com.tamixa.application.admin.StoryNotFoundException
import com.tamixa.application.library.LibraryStoryIllustrationService
import com.tamixa.application.library.LibraryStoryRephraseService
import com.tamixa.application.story.StoryPromptBuilder
import com.tamixa.application.storylibrary.BulkJobStatus
import com.tamixa.application.storylibrary.BulkJobStore
import com.tamixa.application.storylibrary.StoryCategories
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.application.storylibrary.StoryStatus as LibraryStoryStatus
import com.tamixa.application.narration.AdminTtsPreviewService
import com.tamixa.application.narration.StoryProcessingService
import com.tamixa.application.port.ProcessingJobRecord
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.port.TtsMetadataCachePort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.voice.VoiceCloningService
import com.tamixa.application.avatar.AvatarFileTooLargeException
import com.tamixa.application.avatar.AvatarNotFoundException
import com.tamixa.application.avatar.AvatarVideoService
import com.tamixa.application.avatar.FamilyAvatarService
import com.tamixa.application.avatar.InvalidAvatarFileException
import com.tamixa.application.shortcontent.ShortContentGenerationService
import com.tamixa.application.shortcontent.ShortContentService
import com.tamixa.application.subscription.ReferralCodeService
import com.tamixa.application.stream.AudioStreamService
import com.tamixa.api.voice.toResponse
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.application.translation.TranslationService
import com.tamixa.domain.StoryStatus
import com.tamixa.infrastructure.config.AppProperties
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.TaskExecutor
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.lang.management.ManagementFactory
import java.net.InetAddress

@RestController
@RequestMapping("${ApiVersion.V1}/admin")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','REVENUE_ANALYST','CONTENT_MANAGER','SUPPORT')")
class AdminController(
    private val adminService: AdminService,
    private val storyLibraryService: StoryLibraryService,
    private val storyProcessingService: StoryProcessingService,
    @Qualifier("triggerPipelineExecutor") private val triggerPipelineExecutor: TaskExecutor,
    private val storyRepository: StoryRepositoryPort,
    private val libraryStoryIllustrationService: LibraryStoryIllustrationService,
    private val libraryStoryRephraseService: LibraryStoryRephraseService,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    private val audioStreamService: AudioStreamService,
    @Autowired(required = false) private val familyAvatarService: FamilyAvatarService?,
    @Autowired(required = false) private val avatarVideoService: AvatarVideoService?,
    private val appProperties: AppProperties,
    @Autowired(required = false) private val s3Client: S3Client?,
    private val voiceCloningService: VoiceCloningService,
    private val voiceRepository: VoiceRepositoryPort,
    private val referralCodeService: ReferralCodeService,
    private val adminTtsPreviewService: AdminTtsPreviewService,
    private val storyPromptBuilder: StoryPromptBuilder,
    @Autowired(required = false) private val ttsMetadataCache: TtsMetadataCachePort?,
    private val processingJobService: ProcessingJobService,
    @Autowired(required = false) private val voiceCloneAnalytics: VoiceCloneAnalyticsService?,
    @Value("\${spring.flyway.enabled:true}") private val flywayEnabled: Boolean,
    @Value("\${spring.flyway.lock-retry-count:300}") private val flywayLockRetryCount: Int,
    private val bulkJobStore: BulkJobStore,
    private val translationService: TranslationService,
    private val shortContentService: ShortContentService,
    private val shortContentGenerationService: ShortContentGenerationService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val debugLogPath: Path? = System.getenv("DEBUG_LOG_PATH")?.takeIf { it.isNotBlank() }?.let { Path.of(it) }

    // #region agent log
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
            val json = buildString {
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
                json,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
            )
        } catch (_: Exception) {
            // Ignore debug logging failures.
        }
    }

    private fun escapeJson(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun toJsonValue(value: Any?): String = when (value) {
        null -> "null"
        is Number, is Boolean -> value.toString()
        else -> "\"${escapeJson(value.toString())}\""
    }
    // #endregion

    @GetMapping("/analytics/retention")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun getRetention(@RequestParam(defaultValue = "30") days: Int): ResponseEntity<RetentionMetricsDto> =
        ResponseEntity.ok(adminService.getRetentionMetrics(days))

    @GetMapping("/analytics/completion")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun getCompletion(@RequestParam(defaultValue = "30") days: Int): ResponseEntity<CompletionMetricsDto> =
        ResponseEntity.ok(adminService.getCompletionMetrics(days))

    @GetMapping("/analytics/voice-clone")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_VOICE_LOGS')")
    fun getVoiceCloneMetrics(@RequestParam(defaultValue = "30") days: Int): ResponseEntity<com.tamixa.application.analytics.VoiceCloneMetricsDto> =
        ResponseEntity.ok(
            voiceCloneAnalytics?.getMetrics(days.coerceIn(1, 90))
                ?: com.tamixa.application.analytics.VoiceCloneMetricsDto(periodDays = days, voiceCloneCreated = 0, voiceCloneUsed = 0)
        )

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

    @GetMapping("/parents/{id}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun getParent(@PathVariable id: Long): ResponseEntity<ParentDetailDto> {
        val dto = adminService.getParentById(id)
        return if (dto != null) ResponseEntity.ok(dto)
        else ResponseEntity.notFound().build()
    }

    @PostMapping("/parents")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun createParent(
        @Valid @RequestBody request: CreateParentRequest
    ): ResponseEntity<*> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Unit>()
        return try {
            val created = adminService.createParent(adminEmail, request)
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: EmailAlreadyExistsException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to (e.message ?: "Email already registered")))
        } catch (e: PhoneAlreadyInUseException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to (e.message ?: "Phone number already in use by another account")))
        }
    }

    @PutMapping("/parents/{id}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun updateParent(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateParentRequest
    ): ResponseEntity<*> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Unit>()
        return try {
            val updated = adminService.updateParent(adminEmail, id, request)
            ResponseEntity.ok(updated)
        } catch (e: AdminParentNotFoundException) {
            ResponseEntity.notFound().build<Unit>()
        } catch (e: EmailAlreadyExistsException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to (e.message ?: "Email already registered")))
        } catch (e: PhoneAlreadyInUseException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to (e.message ?: "Phone number already in use by another account")))
        }
    }

    @DeleteMapping("/parents/{id}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun deleteParent(@PathVariable id: Long): ResponseEntity<*> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build<Unit>()
        return try {
            adminService.deleteParent(adminEmail, id)
            ResponseEntity.noContent().build<Unit>()
        } catch (e: AdminParentNotFoundException) {
            ResponseEntity.notFound().build<Unit>()
        } catch (e: ParentHasDependentsException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to (e.message ?: "Cannot delete parent with dependents")))
        }
    }

    @GetMapping("/children")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_CHILDREN')")
    fun getChildren(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) parentId: Long?
    ) = ResponseEntity.ok(adminService.getChildren(page, size, parentId))

    @GetMapping("/parents/{parentId}/avatar")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun getParentAvatarUrl(@PathVariable parentId: Long): ResponseEntity<Map<String, String>> {
        val url = familyAvatarService?.getAvatarUrl(parentId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(mapOf("avatarUrl" to url))
    }

    @PostMapping("/parents/{parentId}/avatar")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun uploadParentAvatar(
        @PathVariable parentId: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Map<String, String>> {
        if (familyAvatarService == null) return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(mapOf("message" to "Avatar storage not configured"))
        try {
            familyAvatarService.uploadAvatarForAdmin(parentId, file.bytes, file.contentType ?: "image/jpeg")
            val url = familyAvatarService.getAvatarUrl(parentId)
                ?: throw IllegalStateException("Avatar stored but signed URL failed")
            return ResponseEntity.status(HttpStatus.CREATED).body(mapOf("avatarUrl" to url))
        } catch (e: AvatarFileTooLargeException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to (e.message ?: "File too large")))
        } catch (e: InvalidAvatarFileException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to (e.message ?: "Invalid image format")))
        }
    }

    @DeleteMapping("/parents/{parentId}/avatar")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_PARENTS')")
    fun deleteParentAvatar(@PathVariable parentId: Long): ResponseEntity<Map<String, String>> {
        try {
            familyAvatarService?.deleteAvatarForAdmin(parentId)
            return ResponseEntity.ok(mapOf("message" to "Avatar deleted"))
        } catch (e: AvatarNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to (e.message ?: "No avatar to delete")))
        }
    }

    @GetMapping("/stories/{id}/detail")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_STORIES')")
    fun getStoryDetail(@PathVariable id: Long): ResponseEntity<StoryDetailDto> {
        val dto = adminService.getStoryById(id)
        return if (dto != null) ResponseEntity.ok(dto)
        else ResponseEntity.notFound().build()
    }

    @GetMapping("/stories/summary")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_STORIES')")
    fun getStoriesSummary(
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

    /**
     * List voice profiles for a parent (admin test/support).
     * Exception: any admin role can access (not only VIEW_VOICE_LOGS) for testing.
     */
    @GetMapping("/parents/{parentId}/voice")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT')")
    fun listVoiceProfilesForParent(@PathVariable parentId: Long): ResponseEntity<List<com.tamixa.api.voice.dto.VoiceProfileResponse>> {
        val profiles = voiceRepository.findByParentId(parentId).map { it.toResponse() }
        return ResponseEntity.ok(profiles)
    }

    /**
     * Upload voice reference audio for a parent (admin test/support).
     * Creates a VoiceProfile with reference audio stored in S3 for XTTS/self-hosted cloning.
     * Exception: any admin role can access for testing.
     */
    @PostMapping("/parents/{parentId}/voice/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT')")
    fun uploadVoiceForParent(
        @PathVariable parentId: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<com.tamixa.api.voice.dto.VoiceProfileResponse> {
        if (file.isEmpty) return ResponseEntity.badRequest().build()
        val fileName = file.originalFilename ?: "voice.mp3"
        return try {
            val profile = voiceCloningService.createReferenceVoiceProfileByParentId(parentId, file.bytes, fileName)
            log.info("Admin voice upload: parentId={} profileId={} fileName={}", parentId, profile.id, fileName)
            ResponseEntity.ok(profile.toResponse())
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("Parent not found") == true) ResponseEntity.notFound().build()
            else ResponseEntity.badRequest().build()
        }
    }

    /**
     * Stream reference audio for a voice profile (admin preview of uploaded sample).
     */
    @GetMapping("/parents/{parentId}/voice/{voiceProfileId}/reference-audio")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT')")
    fun getVoiceReferenceAudio(
        @PathVariable parentId: Long,
        @PathVariable voiceProfileId: Long
    ): ResponseEntity<ByteArray> {
        val bytes = voiceCloningService.getReferenceAudioForAdmin(parentId, voiceProfileId)
            ?: return ResponseEntity.notFound().build()
        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType("audio/mpeg")
            contentLength = bytes.size.toLong()
        }
        return ResponseEntity.ok().headers(headers).body(bytes)
    }

    /**
     * Run ElevenLabs voice cloning job for a profile that already has reference audio (no consent required).
     * Use when VOICE_CLONING_PROVIDER=elevenlabs. Upload reference first via POST /parents/{parentId}/voice/upload, then call this.
     */
    @PostMapping("/parents/{parentId}/voice/{voiceProfileId}/run-job")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT')")
    fun runVoiceCloningJobForProfile(
        @PathVariable parentId: Long,
        @PathVariable voiceProfileId: Long
    ): ResponseEntity<Map<String, Any>> {
        return try {
            val job = voiceCloningService.createElevenLabsVoiceCloningJobFromProfile(parentId, voiceProfileId)
            ResponseEntity.ok(
                mapOf(
                    "jobId" to job.id,
                    "status" to job.status.name,
                    "message" to "Voice cloning job started. Wait for processing to complete, then try preview again (cloned:$voiceProfileId with this parentId)."
                ) as Map<String, Any>
            )
        } catch (e: IllegalArgumentException) {
            when {
                e.message?.contains("not found") == true ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to (e.message ?: "Not found")) as Map<String, Any>)
                e.message?.contains("no reference") == true || e.message?.contains("Upload reference first") == true ->
                    ResponseEntity.badRequest().body(mapOf("message" to (e.message)) as Map<String, Any>)
                e.message?.contains("not configured") == true ->
                    ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf("message" to (e.message)) as Map<String, Any>)
                else -> ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Bad request")) as Map<String, Any>)
            }
        }
    }

    /**
     * Upload consent audio and run the Google voice cloning job for a profile that already has reference audio.
     * Use when VOICE_CLONING_PROVIDER=google. Upload reference first via POST /parents/{parentId}/voice/upload, then upload consent here.
     */
    @PostMapping("/parents/{parentId}/voice/{voiceProfileId}/consent", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT')")
    fun uploadConsentAndRunVoiceCloning(
        @PathVariable parentId: Long,
        @PathVariable voiceProfileId: Long,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<Map<String, Any>> {
        if (file.isEmpty) return ResponseEntity.badRequest().body(mapOf("message" to "Consent audio file is required") as Map<String, Any>)
        val fileName = file.originalFilename ?: "consent.mp3"
        return try {
            val job = voiceCloningService.createGoogleVoiceCloningJobFromProfile(parentId, voiceProfileId, file.bytes, fileName)
            ResponseEntity.ok(
                mapOf(
                    "jobId" to job.id,
                    "status" to job.status.name,
                    "message" to "Voice cloning job started. Wait for processing to complete, then try preview again (cloned:$voiceProfileId with this parentId)."
                ) as Map<String, Any>
            )
        } catch (e: IllegalArgumentException) {
            when {
                e.message?.contains("not found") == true ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to (e.message ?: "Not found")) as Map<String, Any>)
                e.message?.contains("no reference") == true || e.message?.contains("Upload reference first") == true ->
                    ResponseEntity.badRequest().body(mapOf("message" to (e.message)) as Map<String, Any>)
                e.message?.contains("not configured") == true ->
                    ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(mapOf("message" to (e.message)) as Map<String, Any>)
                else -> ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Bad request")) as Map<String, Any>)
            }
        }
    }

    /**
     * Delete a voice profile for a parent so admin can upload an alternate sample.
     */
    @DeleteMapping("/parents/{parentId}/voice/{voiceProfileId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT')")
    fun deleteVoiceProfileForParent(
        @PathVariable parentId: Long,
        @PathVariable voiceProfileId: Long
    ): ResponseEntity<Map<String, Any>> {
        return try {
            voiceCloningService.deleteReferenceVoiceProfileByParentId(parentId, voiceProfileId)
            ResponseEntity.ok(mapOf("message" to "Voice profile deleted", "voiceProfileId" to voiceProfileId))
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("not found") == true)
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to (e.message ?: "Not found")))
            else
                ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Bad request")))
        }
    }

    /**
     * Set HeyGen voice_id on a voice profile so cloned narration uses HeyGen TTS.
     * Create the voice in HeyGen app first, then pass the voice_id here.
     */
    @PatchMapping("/parents/{parentId}/voice/{voiceProfileId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT')")
    fun patchVoiceProfileHeyGenVoiceId(
        @PathVariable parentId: Long,
        @PathVariable voiceProfileId: Long,
        @Valid @RequestBody request: PatchVoiceProfileRequest
    ): ResponseEntity<com.tamixa.api.voice.dto.VoiceProfileResponse> {
        val heygenVoiceId = request.heygenVoiceId?.trim()?.takeIf { it.isNotBlank() }
        val updated = voiceCloningService.updateHeyGenVoiceId(parentId, voiceProfileId, heygenVoiceId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated.toResponse())
    }

    @GetMapping("/subscriptions")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_SUBSCRIPTIONS')")
    fun getSubscriptionStatus(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ) = ResponseEntity.ok(adminService.getSubscriptionStatus(page, size))

    @GetMapping("/referral-codes")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun getReferralCodes(): ResponseEntity<List<ReferralCodeDto>> {
        val list = referralCodeService.findAll().map { toReferralCodeDto(it) }
        return ResponseEntity.ok(list)
    }

    @GetMapping("/referral-codes/{id}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun getReferralCode(@PathVariable id: Long): ResponseEntity<ReferralCodeDto> {
        val code = referralCodeService.findById(id) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(toReferralCodeDto(code))
    }

    @PostMapping("/referral-codes")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun createReferralCode(@Valid @RequestBody request: CreateReferralCodeRequest): ResponseEntity<ReferralCodeDto> {
        val created = referralCodeService.create(
            shortcode = request.shortcode,
            shopName = request.shopName,
            offerPercent = request.offerPercent,
            expiresAt = request.expiresAt,
            active = request.active
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(toReferralCodeDto(created))
    }

    @PutMapping("/referral-codes/{id}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun updateReferralCode(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateReferralCodeRequest
    ): ResponseEntity<ReferralCodeDto> {
        val updated = referralCodeService.update(
            id = id,
            shortcode = request.shortcode,
            shopName = request.shopName,
            offerPercent = request.offerPercent,
            expiresAt = request.expiresAt,
            active = request.active
        )
        return ResponseEntity.ok(toReferralCodeDto(updated))
    }

    @DeleteMapping("/referral-codes/{id}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_REVENUE')")
    fun deleteReferralCode(@PathVariable id: Long): ResponseEntity<Unit> {
        referralCodeService.delete(id)
        return ResponseEntity.noContent().build()
    }

    // region Short content (riddles, thought for the day, proverbs, tongue twisters, etc.)
    @GetMapping("/short-content")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_STORIES')")
    fun getShortContent(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) language: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<org.springframework.data.domain.Page<ShortContentDto>> {
        val result = shortContentService.listForAdmin(type, language, status, page, size)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/short-content/{id}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_STORIES')")
    fun getShortContentById(@PathVariable id: Long): ResponseEntity<ShortContentDto> {
        val dto = shortContentService.findByIdForAdmin(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/short-content")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_STORIES')")
    fun createShortContent(@Valid @RequestBody request: CreateShortContentRequest): ResponseEntity<ShortContentDto> {
        val created = shortContentService.create(
            type = request.type,
            content = request.content,
            answer = request.answer,
            language = request.language,
            ageMin = request.ageMin,
            ageMax = request.ageMax,
            displayDate = request.displayDate,
            audioUrl = request.audioUrl,
            status = request.status
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/short-content/{id}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_STORIES')")
    fun updateShortContent(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateShortContentRequest
    ): ResponseEntity<ShortContentDto> {
        val updated = shortContentService.update(
            id = id,
            type = request.type,
            content = request.content,
            answer = request.answer,
            language = request.language,
            ageMin = request.ageMin,
            ageMax = request.ageMax,
            displayDate = request.displayDate,
            audioUrl = request.audioUrl,
            status = request.status
        )
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/short-content/{id}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_STORIES')")
    fun deleteShortContent(@PathVariable id: Long): ResponseEntity<Unit> {
        if (!shortContentService.delete(id)) return ResponseEntity.notFound().build()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/short-content/generate")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_STORIES')")
    fun generateShortContent(@Valid @RequestBody request: GenerateShortContentRequest): ResponseEntity<GenerateShortContentResponse> {
        val result = shortContentGenerationService.generate(request.type, request.language, request.count)
        return ResponseEntity.ok(GenerateShortContentResponse(items = result.items, rejectedCount = result.rejectedCount))
    }
    // endregion

    private fun toReferralCodeDto(code: com.tamixa.domain.ReferralCode): ReferralCodeDto = ReferralCodeDto(
        id = code.id,
        shortcode = code.shortcode,
        shopName = code.shopName,
        offerPercent = code.offerPercent,
        expiresAt = code.expiresAt,
        active = code.active,
        stripeCouponId = code.stripeCouponId,
        createdAt = code.createdAt,
        updatedAt = code.updatedAt
    )

    @GetMapping("/health")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_HEALTH')")
    fun getHealth() = ResponseEntity.ok(adminService.getHealth())

    /**
     * Runtime migration mode visibility for operators.
     * Helps avoid accidentally running multiple Flyway-enabled app nodes.
     */
    @GetMapping("/system/runtime-config")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_HEALTH')")
    fun getRuntimeConfig(): ResponseEntity<Map<String, Any>> {
        val host = runCatching { InetAddress.getLocalHost().hostName }.getOrDefault("unknown-host")
        val pid = runCatching { ManagementFactory.getRuntimeMXBean().name.substringBefore("@") }.getOrDefault("unknown-pid")
        return ResponseEntity.ok(
            mapOf(
                "instanceId" to "$host:$pid",
                "flywayEnabled" to flywayEnabled,
                "flywayLockRetryCount" to flywayLockRetryCount,
                "migrationMode" to if (flywayEnabled) "MIGRATION_ENABLED" else "APP_ONLY",
                "operatorHint" to if (flywayEnabled) {
                    "This instance can run DB migrations. Keep exactly one migration-enabled instance during rollout."
                } else {
                    "App-only mode (recommended for horizontally scaled staging/prod nodes)."
                }
            )
        )
    }

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

    @GetMapping("/stories/pending-review")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getStoriesPendingReview(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<com.tamixa.api.admin.dto.LibraryStoryResponse>> {
        val pageResult = storyLibraryService.findPendingNarrationReview(page, size)
        return ResponseEntity.ok(
            PagedResponse(
                content = pageResult.content.map { story ->
                    val resolvedAudio = story.audioFileUrl?.takeIf { it.isNotBlank() }
                        ?: storyLibraryService.resolveAudioFileUrlForDisplay(story.id, story.language)
                    val translationApproval = storyLibraryService.getTranslationNarrationApproval(story.id)
                    story.toResponse(
                        coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                        coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
                    ).copy(audioFileUrl = resolvedAudio, translationApproval = translationApproval)
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

    /**
     * Approved stories for Narration tab: list content-approved stories so admin can trigger TTS pipeline.
     * Path /narration/approved-stories avoids any conflict with /stories/{id}.
     */
    @GetMapping("/narration/approved-stories")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getApprovedStoriesForNarration(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<com.tamixa.api.admin.dto.LibraryStoryResponse>> {
        val pageResult = storyLibraryService.findApprovedForSpeech(page, size)
        return ResponseEntity.ok(
            PagedResponse(
                content = pageResult.content.map { story ->
                    val resolvedAudio = story.audioFileUrl?.takeIf { it.isNotBlank() }
                        ?: storyLibraryService.resolveAudioFileUrlForDisplay(story.id, story.language)
                    val translationApproval = storyLibraryService.getTranslationNarrationApproval(story.id)
                    story.toResponse(
                        coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                        coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
                    ).copy(audioFileUrl = resolvedAudio, translationApproval = translationApproval)
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

    /**
     * Approved stories for "Story to Speech": list content-approved stories so admin can trigger TTS pipeline.
     * Use when audio-after-approval=true (Process 1: story creation, Process 2: approval, Process 3: audio from this menu).
     * @deprecated Prefer GET /narration/approved-stories to avoid path conflicts; this endpoint kept for backward compatibility.
     */
    @GetMapping("/stories/to-speech")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getStoriesToSpeech(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedResponse<com.tamixa.api.admin.dto.LibraryStoryResponse>> {
        val pageResult = storyLibraryService.findApprovedForSpeech(page, size)
        return ResponseEntity.ok(
            PagedResponse(
                content = pageResult.content.map { story ->
                    val resolvedAudio = story.audioFileUrl?.takeIf { it.isNotBlank() }
                        ?: storyLibraryService.resolveAudioFileUrlForDisplay(story.id, story.language)
                    val translationApproval = storyLibraryService.getTranslationNarrationApproval(story.id)
                    story.toResponse(
                        coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                        coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
                    ).copy(audioFileUrl = resolvedAudio, translationApproval = translationApproval)
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

    @GetMapping("/stories")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getLibraryStories(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<PagedResponse<com.tamixa.api.admin.dto.LibraryStoryResponse>> {
        val pageResult = storyLibraryService.findAllByStatus(status, page, size)
        return ResponseEntity.ok(
            PagedResponse(
                content = pageResult.content.map { story ->
                    val resolvedAudio = story.audioFileUrl?.takeIf { it.isNotBlank() }
                        ?: storyLibraryService.resolveAudioFileUrlForDisplay(story.id, story.language)
                    val translationApproval = storyLibraryService.getTranslationNarrationApproval(story.id)
                    story.toResponse(
                        coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                        coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
                    ).copy(audioFileUrl = resolvedAudio, translationApproval = translationApproval)
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
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getStoryStatus(@PathVariable id: Long): ResponseEntity<Map<String, String>> =
        getLibraryStoryPipelineStatus(id)

    @GetMapping("/stories/{id}")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getLibraryStory(
        @PathVariable id: Long,
        @RequestParam(required = false) language: String?
    ): ResponseEntity<com.tamixa.api.admin.dto.LibraryStoryResponse> {
        val story = storyLibraryService.findById(id) ?: return ResponseEntity.notFound().build()
        val effectiveLang = when (val requested = language?.trim()?.lowercase()?.take(10)) {
            null, "" -> normalizeLanguageForAdmin(storyLibraryService.resolveLanguageForAdminEdit(story))
            else -> normalizeLanguageForAdmin(requested)
        }
        val response = storyLibraryService.findByIdAndLanguage(id, effectiveLang)
        if (response != null) {
            return ResponseEntity.ok(response)
        }
        if (language != null) return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            story.toResponse(
                coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
            )
        )
    }

    private fun normalizeLanguageForAdmin(lang: String): String {
        val map = mapOf(
            "tamil" to "ta", "english" to "en", "hindi" to "hi",
            "telugu" to "te", "kannada" to "kn", "malayalam" to "ml"
        )
        return map[lang] ?: lang
    }

    @PutMapping("/stories/{id}/translations/{language}")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun updateCuratedStoryTranslation(
        @PathVariable id: Long,
        @PathVariable language: String,
        @Valid @RequestBody request: UpdateTranslationRequest
    ): ResponseEntity<*> {
        val updated = storyLibraryService.updateTranslationContent(
            masterStoryId = id,
            language = language,
            title = request.title,
            content = request.content,
            moral = request.moral
        )
        return if (updated) ResponseEntity.ok(mapOf("message" to "Translation updated"))
        else ResponseEntity.notFound().build<Map<String, String>>()
    }

    @PostMapping("/stories/{id}/translations/{language}/approve-narration")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun approveTranslationNarration(
        @PathVariable id: Long,
        @PathVariable language: String
    ): ResponseEntity<*> {
        val ok = storyLibraryService.approveTranslationNarration(id, language)
        if (ok) {
            triggerPipelineExecutor.execute {
                try {
                    storyProcessingService.processSync(id)
                    log.info("Pipeline completed after per-language approval for story id={}", id)
                } catch (e: Exception) {
                    log.error("Pipeline failed after approval for story id={}: {}", id, e.message, e)
                }
            }
            return ResponseEntity.ok(mapOf("message" to "Narration approved for language; pipeline running in background"))
        }
        return ResponseEntity.notFound().build<Map<String, String>>()
    }

    @PostMapping("/stories/{id}/regenerate-cover")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun regenerateCuratedStoryCover(
        @PathVariable id: Long,
        @org.springframework.web.bind.annotation.RequestParam(name = "force", required = false, defaultValue = "true") force: Boolean
    ): ResponseEntity<*> {
        log.info("Regenerate cover requested for curated story id={} force={}", id, force)
        val story = storyLibraryService.findById(id) ?: return ResponseEntity.notFound().build<Unit>()
        val updated = libraryStoryIllustrationService.generateCoverForStory(story, force)
        return if (updated != null) {
            log.info("Regenerate cover success for curated story id={}", id)
            ResponseEntity.ok(
                updated.toResponse(
                    coverImageUrlResolver.resolveCoverPath(updated.coverImageUrl),
                    coverImageUrlResolver.resolveCoverVideoPath(updated.coverVideoUrl)
                )
            )
        } else {
            log.warn("Regenerate cover failed for curated story id={} (DALL-E returned null or S3 storage failed)", id)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(mapOf("message" to "Regenerate cover failed (OpenAI blocked prompt, API unavailable, or storage unavailable). Try retrying with a safer story description."))
        }
    }

    @PostMapping("/stories/{id}/suggest-rephrase")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun suggestRephrase(
        @PathVariable id: Long,
        @Valid @RequestBody request: SuggestRephraseRequest
    ): ResponseEntity<*> {
        val story = storyLibraryService.findById(id) ?: return ResponseEntity.notFound().build<Unit>()
        val title = request.title?.takeIf { it.isNotBlank() } ?: story.title
        val content = request.content?.takeIf { it.isNotBlank() } ?: story.content
        if (content.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Content is required"))
        }
        val suggestion = libraryStoryRephraseService.suggestRephrase(title, content)
        return if (suggestion != null) ResponseEntity.ok(mapOf(
            "suggestedTitle" to (suggestion.suggestedTitle ?: ""),
            "suggestedContent" to suggestion.suggestedContent
        ))
        else ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(mapOf("message" to "Rephrase suggestion failed (OpenAI unavailable)"))
    }

    /**
     * Regenerate story content with the default Tamixa conversion prompt (LLM rewrite).
     * Returns transformed content, title, and moral so the admin can preview in the edit form before saving or submitting for review.
     * When generateForAllLanguages is true (default), also translates the transformed content to all target languages (en, hi, te, kn, ml)
     * and returns them in "translations"; if a language had no story content, it will be created. Does not persist; use Save or Submit for review after.
     */
    @PostMapping("/stories/{id}/regenerate-with-prompt")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun regenerateStoryWithPrompt(
        @PathVariable id: Long,
        @RequestBody(required = false) body: Map<String, Any?>?
    ): ResponseEntity<*> {
        val story = storyLibraryService.findById(id) ?: return ResponseEntity.notFound().build<Unit>()
        val contentStr = body?.get("content")?.toString()?.trim()?.takeIf { it.isNotBlank() } ?: story.content
        if (contentStr.isBlank()) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Content is required"))
        }
        val generateForAllLanguages = when (val v = body?.get("generateForAllLanguages")) {
            is Boolean -> v
            is String -> v.equals("true", ignoreCase = true)
            else -> true
        }
        return try {
            val outputLang = story.language.trim().lowercase().takeIf { it.isNotBlank() }
            val prompt = storyPromptBuilder.buildDefaultConversionPrompt(StoryCategories.canonical, outputLang)
            val transformed = adminTtsPreviewService.transformContent(contentStr, prompt)
            val response = mutableMapOf<String, Any>(
                "content" to transformed.content,
                "title" to (transformed.title ?: ""),
                "moral" to (transformed.moral ?: "")
            )
            transformed.category?.let { response["category"] = it }
            transformed.theme?.let { response["theme"] = it }
            if (generateForAllLanguages) {
                val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
                val targetLangs = appProperties.translationPipeline.targetLanguages
                    .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() && it != sourceLang }
                val translationsMap = mutableMapOf<String, Map<String, String>>()
                for (targetLang in targetLangs) {
                    try {
                        val result = translationService.translateIfNeeded(
                            sourceLang = sourceLang,
                            targetLang = targetLang,
                            title = transformed.title,
                            content = transformed.content,
                            moral = transformed.moral
                        )
                        translationsMap[targetLang] = mapOf(
                            "content" to (result.content.take(50_000)),
                            "title" to (result.title?.take(500) ?: ""),
                            "moral" to (result.moral?.take(1000) ?: "")
                        )
                    } catch (e: Exception) {
                        log.warn("Regenerate translate failed for story id={} targetLang={}: {}", id, targetLang, e.message)
                        translationsMap[targetLang] = mapOf(
                            "content" to "",
                            "title" to "",
                            "moral" to ""
                        )
                    }
                }
                response["translations"] = translationsMap
            }
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            log.warn("Regenerate with prompt failed for story id={}: {}", id, e.message)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to ("Regeneration failed: " + (e.message ?: "Unknown error"))))
        }
    }

    @GetMapping("/stories/with-issues")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getStoriesWithIssues(): ResponseEntity<List<com.tamixa.application.storylibrary.StoryWithIssues>> =
        ResponseEntity.ok(storyLibraryService.getStoriesWithIssues())

    /** Batch pipeline status for multiple stories. Reduces N requests to 1 when admin polls status for table rows. */
    @GetMapping("/pipeline/status-batch")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getLibraryStoryPipelineStatusBatch(
        @RequestParam ids: List<Long>
    ): ResponseEntity<Map<Long, Map<String, String>>> {
        if (ids.isEmpty()) return ResponseEntity.ok(emptyMap())
        val limit = 100
        val limited = ids.distinct().take(limit)
        val statuses = storyLibraryService.getPipelineStatusByStoryIds(limited)
        return ResponseEntity.ok(statuses)
    }

    @GetMapping("/stories/{id}/pipeline-status")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getLibraryStoryPipelineStatus(
        @PathVariable id: Long
    ): ResponseEntity<Map<String, String>> {
        val status = storyLibraryService.getPipelineStatusByStoryId(id)
        return if (status.isEmpty()) ResponseEntity.notFound().build()
        else ResponseEntity.ok(status)
    }

    /**
     * Set or clear reject-marked flag (admin ticked "Reject this story" in language view).
     * Enables the common Reject button when marked.
     */
    @PatchMapping("/stories/{id}/reject-marked")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun setRejectMarked(
        @PathVariable id: Long,
        @Valid @RequestBody request: com.tamixa.api.admin.dto.SetRejectMarkedRequest
    ): ResponseEntity<Map<String, Any>> {
        return if (storyLibraryService.setRejectMarked(id, request.marked)) {
            ResponseEntity.ok(mapOf("storyId" to id, "rejectMarked" to request.marked))
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Mark one or more languages as reviewed for a story.
     * Persisted in DB. Enables Approve when all pipeline languages are reviewed (via "Have reviewed" in View modal).
     */
    @PostMapping("/stories/{id}/reviewed-languages")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun markReviewedLanguages(
        @PathVariable id: Long,
        @Valid @RequestBody request: com.tamixa.api.admin.dto.MarkReviewedLanguagesRequest
    ): ResponseEntity<Map<String, Any>> {
        val story = storyLibraryService.findById(id) ?: return ResponseEntity.notFound().build()
        storyLibraryService.markLanguagesReviewed(id, request.languages)
        return ResponseEntity.ok(mapOf("storyId" to id, "languages" to request.languages))
    }

    /**
     * Returns whether any story pipeline is currently running (translate/TTS),
     * and which stories/languages are being processed (for banner).
     * ageMinutes: minutes since that story started (for "Clear stuck" when > claim-max-age-minutes).
     */
    @GetMapping("/pipeline/active-now")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun isPipelineActive(): ResponseEntity<Map<String, Any>> {
        val active = storyLibraryService.isPipelineActive()
        val activeStories = storyLibraryService.getActiveStories().map { (id, lang) ->
            mapOf(
                "storyId" to id,
                "language" to lang,
                "ageMinutes" to (storyLibraryService.getPipelineAgeMinutes(id) ?: 0)
            )
        }
        return ResponseEntity.ok(mapOf("active" to active, "activeStories" to activeStories))
    }

    /**
     * Clear stuck pipeline entry so admin can retry via Run pipeline.
     * Only clears if entry is older than claim-max-age-minutes (avoids killing active runs).
     */
    @PostMapping("/pipeline/clear-stuck/{storyId}")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun clearStuckPipeline(@PathVariable storyId: Long): ResponseEntity<Map<String, Any>> {
        val cleared = storyLibraryService.clearStuckPipeline(storyId)
        return ResponseEntity.ok(mapOf(
            "cleared" to cleared,
            "message" to (if (cleared) "Pipeline slot cleared. You can now Run pipeline for this story."
                else "Pipeline may still be running (entry too recent). Wait or check backend logs.")
        ))
    }

    /**
     * Reset narration status for a story so it shows as Pending. Use when audio is missing in S3 but status still shows "Done".
     * After reset, run "Regenerate audio" to generate audio again.
     */
    @PostMapping("/stories/{id}/reset-narration-status")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun resetNarrationStatus(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val cleared = storyLibraryService.resetNarrationStatusForStory(id)
        return ResponseEntity.ok(mapOf(
            "cleared" to cleared,
            "message" to (if (cleared > 0) "Audio status reset. Click Regenerate audio to generate again."
                else "Story not found or no translations to reset.")
        ))
    }

    /**
     * Get processing job by ID. For AI workflow tracking (story polish, TTS, voice cloning, avatar).
     */
    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getJobById(@PathVariable jobId: Long): ResponseEntity<*> {
        val job = processingJobService.getById(jobId)
        return if (job != null) ResponseEntity.ok(jobToMap(job))
        else ResponseEntity.notFound().build<Unit>()
    }

    /**
     * Get processing jobs for a resource. E.g. resourceType=curated_story, resourceId=42.
     */
    @GetMapping("/jobs")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getJobs(
        @RequestParam(required = false) resourceType: String?,
        @RequestParam(required = false) resourceId: String?,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<*> {
        val jobs = when {
            resourceType != null && resourceId != null ->
                processingJobService.getByResource(resourceType, resourceId)
            else ->
                processingJobService.getRecent(limit)
        }
        return ResponseEntity.ok(jobs.map { jobToMap(it) })
    }

    private fun jobToMap(job: ProcessingJobRecord): Map<String, Any?> = mapOf(
        "id" to job.id,
        "jobType" to job.jobType,
        "resourceType" to job.resourceType,
        "resourceId" to job.resourceId,
        "status" to job.status,
        "progress" to job.progress,
        "startedAt" to job.startedAt?.toString(),
        "finishedAt" to job.finishedAt?.toString(),
        "errorMessage" to job.errorMessage,
        "createdAt" to job.createdAt.toString(),
        "updatedAt" to job.updatedAt.toString()
    )

    /**
     * Returns a playable stream URL for admin preview of narration audio.
     * Used to verify translation quality and conversational tone (vs flat reading).
     * For testing: pass voiceProfile (e.g. cloned:1) and parentId to get URL for cloned voice.
     */
    @GetMapping("/stories/{id}/stream-url")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun getLibraryStoryStreamUrl(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) voiceProfile: String?,
        @RequestParam(required = false) parentId: Long?
    ): ResponseEntity<Map<String, String>> {
        val url = when {
            voiceProfile != null && voiceProfile.isNotBlank() && parentId != null ->
                audioStreamService.getLibraryNarrationStreamUrl(id, language, voiceProfile.trim(), parentId)
            else ->
                audioStreamService.getLibraryStreamUrl(id, language, null)
        }
        if (url == null) return ResponseEntity.notFound().build()
        val voice = voiceProfile?.takeIf { it.isNotBlank() } ?: "default"
        val body = mutableMapOf("streamUrl" to url)
        if (parentId != null) {
            familyAvatarService?.getAvatarUrl(parentId)?.let { body["avatarUrl"] = it }
            avatarVideoService?.getAvatarVideoUrl(id, "library", parentId, language, voice)?.let { body["avatarVideoUrl"] = it }
            avatarVideoService?.getAvatarVideoStatus(id, "library", parentId, language, voice)?.let { (status, errorMsg) ->
                body["avatarVideoStatus"] = status!!.name
                if (errorMsg != null) body["avatarVideoError"] = errorMsg
            }
            avatarVideoService?.getAvatarVideoProviderInfo()?.let { body["avatarVideoProvider"] = it }
        }
        return ResponseEntity.ok(body)
    }

    /**
     * Deletes the avatar video for the given curated story + parent + language + voice so it can be regenerated.
     * Requires parentId and voiceProfile (e.g. cloned:1) in query params.
     */
    @DeleteMapping("/stories/{id}/avatar-video")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun deleteLibraryStoryAvatarVideo(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) voiceProfile: String?,
        @RequestParam(required = false) parentId: Long?
    ): ResponseEntity<Unit> {
        if (parentId == null || voiceProfile.isNullOrBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val deleted = avatarVideoService?.deleteAvatarVideo(id, "library", parentId, language, voiceProfile) == true
        return if (deleted) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    /**
     * Streams narration audio bytes for admin preview.
     * Use this URL for Audio playback so it works regardless of AUDIO_PUBLIC_BASE_URL
     * (e.g. when set for Android emulator 10.0.2.2, browser cannot reach it).
     * For testing cloned voice: pass voiceProfile (e.g. cloned:1) and parentId.
     */
    @GetMapping("/stories/{id}/preview-audio")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun streamCuratedStoryAudio(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) voiceProfile: String?,
        @RequestParam(required = false) parentId: Long?
    ): ResponseEntity<Any> {
        val useCloned = voiceProfile != null && voiceProfile.isNotBlank() && parentId != null
        if (useCloned) {
            log.info("Admin preview-audio: storyId={} lang={} voiceProfile={} parentId={} (cloned)", id, language, voiceProfile, parentId)
        }
        val key = try {
            when {
                useCloned ->
                    audioStreamService.getLibraryNarrationStoragePath(id, language, voiceProfile!!.trim(), parentId!!)
                else ->
                    storyLibraryService.getNarrationStoragePath(id, language)
            }
        } catch (e: com.tamixa.infrastructure.voice.XttsUnavailableException) {
            log.warn("Admin stream: XTTS unavailable for cloned voice: {}", e.message)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to (e.message ?: "Cloned voice synthesis unavailable. XTTS service requires Python 3.9–3.11 and Coqui TTS.")))
        }
        if (key == null) {
            if (useCloned) {
                log.warn("Admin stream: cloned voice audio not ready for story {} lang={} voiceProfile={} parentId={}", id, language, voiceProfile, parentId)
                val effectiveLang = com.tamixa.application.stream.StreamLanguageUtils.normalize(language)
                val msg = if (effectiveLang == "ta") {
                    "Tamil cloned voice failed: this profile has no cloned voice key. In Admin → Voice & Avatar Studio: (1) Upload reference audio for this parent, (2) Run job (ElevenLabs) or upload consent + Run job (Google). Backend .env: VOICE_CLONING_ENABLED=true, VOICE_CLONING_PROVIDER=elevenlabs + ELEVENLABS_API_KEY, or provider=google + GOOGLE_CLOUD_TTS_API_KEY. Restart backend after changing .env."
                } else {
                    "Cloned voice audio not ready for this language. Republish the story and try again."
                }
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapOf("message" to msg))
            }
            log.warn("Admin stream: no audio for story {} lang={} (pipeline not run or not ready)", id, language)
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf(
                    "message" to "Audio not ready for this language. Go to Narration (Story to Speech), run Generate audio or Regenerate audio for this story, wait for the pipeline to finish, then try preview again."
                ))
        }
        val client = s3Client ?: run {
            log.warn("Admin stream: audio storage not configured (S3 client null) storyId={} lang={}", id, language)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Audio storage (S3) is not configured. Set storage credentials in backend .env and restart."))
        }
        val bucket = appProperties.storage.effectiveS3Bucket
        return try {
            val response = client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())
            val bytes = response.readAllBytes()
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .body(bytes)
        } catch (e: NoSuchKeyException) {
            log.warn("Admin stream: audio not found in S3 key={} storyId={} lang={}", key, id, language, e)
            try {
                var cleared = storyLibraryService.clearStaleNarrationByStorageKey(id, key)
                if (!cleared) {
                    cleared = storyLibraryService.clearStaleNarrationAudioForLanguage(id, language)
                    if (!cleared && key.startsWith("stories/")) {
                        val segments = key.split("/")
                        if (segments.size >= 3) {
                            val langFromKey = segments[2].trim().lowercase()
                            if (langFromKey.isNotBlank()) cleared = storyLibraryService.clearStaleNarrationAudioForLanguage(id, langFromKey)
                        }
                    }
                }
            } catch (clearEx: Exception) {
                log.warn("Failed to clear stale narration for story {} key={}: {}", id, key, clearEx.message)
            }
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Audio file missing in storage. Go to Narration → Regenerate audio for this story, then try preview again."))
        } catch (e: Exception) {
            log.error("Admin stream failed storyId={} lang={}: {}", id, language, e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to (e.message ?: "Stream failed. Please try again.")))
        }
    }

    /**
     * Generate TTS audio for a story: direct (content as-is) or with custom prompt (transform then TTS).
     * POST body: { "language": "ta", "prompt": null } for direct TTS; set "prompt" to transform story with LLM first.
     * Returns audio/mpeg bytes for playback in admin.
     */
    @PostMapping("/stories/{id}/tts-preview")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun generateTtsPreview(
        @PathVariable id: Long,
        @Valid @RequestBody request: TtsPreviewRequest?
    ): ResponseEntity<Any> {
        val lang = request?.language?.trim()?.take(10)?.ifEmpty { "ta" } ?: "ta"
        val prompt = request?.prompt?.trim()?.takeIf { it.isNotBlank() }
        return try {
            val bytes = if (prompt.isNullOrBlank()) {
                adminTtsPreviewService.generateDirectTts(id, lang)
            } else {
                adminTtsPreviewService.generatePromptAndTts(id, lang, prompt)
            }
            when {
                bytes == null || bytes.isEmpty() ->
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapOf("message" to "Story not found or content empty. Check story ID and language."))
                else ->
                    ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/mpeg"))
                        .header(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
                        .body(bytes)
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to (e.message ?: "Invalid request")))
        } catch (e: Exception) {
            log.warn("Admin TTS preview failed storyId={} lang={}: {}", id, lang, e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to (e.message ?: "TTS preview failed. Check OPENAI_API_KEY for prompt mode and TTS provider.")))
        }
    }

    /**
     * Deletes stored narration for a curated story + language + voice so the next preview regenerates it.
     * For cloned voice pass voiceProfile (e.g. cloned:1) and parentId. Requires MANAGE_STORIES (story library).
     */
    @DeleteMapping("/stories/{id}/narration-audio")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun deleteCuratedStoryNarrationAudio(
        @PathVariable id: Long,
        @RequestParam language: String,
        @RequestParam voiceProfile: String,
        @RequestParam(required = false) parentId: Long?
    ): ResponseEntity<Any> {
        return try {
            val deleted = audioStreamService.deleteLibraryStoryNarrationAudio(id, language, voiceProfile.trim(), parentId)
            if (deleted) {
                ResponseEntity.ok().body(mapOf("deleted" to true, "message" to "Narration audio removed. Next preview will regenerate."))
            } else {
                ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("deleted" to false, "message" to "No narration audio found for this story, language, and voice."))
            }
        } catch (e: Exception) {
            log.warn("Admin delete narration failed storyId={} lang={} voice={}: {}", id, language, voiceProfile, e.message)
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("deleted" to false, "message" to (e.message ?: "Delete failed.")))
        }
    }

    /**
     * Streams TTS narration audio for admin preview of user-generated stories.
     * Uses story.audioFileUrl (conversational narration from GeneratedStoryNarrationService).
     */
    @GetMapping("/stories/{id}/preview-audio/generated")
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
        val client = s3Client ?: run {
            log.warn("Admin stream: audio storage not configured (S3 client null) generatedStoryId={}", id)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Something went wrong. Please try again."))
        }
        val bucket = appProperties.storage.effectiveS3Bucket
        return try {
            val response = client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())
            val bytes = response.readAllBytes()
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_LENGTH, bytes.size.toString())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .body(bytes)
        } catch (e: NoSuchKeyException) {
            log.warn("Admin stream: generated story audio not found in S3 key={} storyId={}", key, id, e)
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Something went wrong. Please try again."))
        } catch (e: Exception) {
            log.error("Admin stream failed generated storyId={}: {}", id, e.message, e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("message" to "Something went wrong. Please try again."))
        }
    }

    @PutMapping("/stories/{id}")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun updateCuratedStory(
        @PathVariable id: Long,
        @Valid @RequestBody request: com.tamixa.api.admin.dto.CreateLibraryStoryRequest
    ): ResponseEntity<*> {
        log.info("Admin update curated story id={} status={} regenerateNarration={} pipelineOnSubmitOnly={}", id, request.status, request.regenerateNarration, appProperties.translationPipeline.pipelineOnSubmitOnly)
        val pipelineOnSubmitOnly = appProperties.translationPipeline.pipelineOnSubmitOnly
        val runPipelineOnUpdate = !pipelineOnSubmitOnly && (request.regenerateNarration != false)
        // When pipelineOnSubmitOnly=true and status=PUBLISHED: Submit for review path triggers pipeline below (else if)
        val existingStory = storyLibraryService.findById(id)
        val previousStatus = existingStory?.status
        val story = storyLibraryService.update(
                id = id,
                title = request.title,
                content = request.content,
                theme = request.theme,
                category = request.category,
                language = request.language.ifBlank { "ta" },
                age = request.age,
                childName = request.childName.ifBlank { "Child" },
                moral = request.moral,
                status = request.status.ifBlank { LibraryStoryStatus.DRAFT },
                coverImageUrl = request.coverImageUrl?.takeIf { it.isNotBlank() }?.let { coverImageUrlResolver.normalizeForStorage(it) ?: it },
                coverVideoUrl = request.coverVideoUrl?.takeIf { it.isNotBlank() }?.let { coverImageUrlResolver.normalizeForStorage(it) ?: it },
                emotionMode = request.emotionMode,
                updateNarratedOnly = request.status == LibraryStoryStatus.PUBLISHED && !runPipelineOnUpdate,
                translationContents = request.translationContents?.takeIf { it.isNotEmpty() },
                translationContentEntries = request.translationContentEntries?.takeIf { it.isNotEmpty() },
                convertPromptUsed = null
            )
        if (story != null) {
            if (story.status == LibraryStoryStatus.PUBLISHED && runPipelineOnUpdate) {
                log.info("Admin curated story id={} is PUBLISHED (pipelineOnSubmitOnly=false), triggering invalidate & reprocess in background", id)
                triggerPipelineExecutor.execute {
                    try {
                        storyProcessingService.invalidateAndReprocess(id)
                        log.info(">>> PIPELINE TASK COMPLETED (Update & publish) storyId={}", id)
                    } catch (e: Exception) {
                        log.error(">>> PIPELINE TASK FAILED (Update & publish) storyId={} error={}", id, e.message, e)
                    }
                }
            } else if (story.status == LibraryStoryStatus.PUBLISHED && !runPipelineOnUpdate) {
                // Submit for review: when audioAfterApproval=false, trigger pipeline (translate + TTS). When audioAfterApproval=true, do not trigger—use "Story to Speech" after approval.
                val audioAfterApproval = appProperties.translationPipeline.audioAfterApproval
                if (!audioAfterApproval) {
                    val isResubmit = previousStatus == LibraryStoryStatus.CHANGES_REQUESTED
                    log.info("PIPELINE >>> Submit for review{}: triggering pipeline for storyId={} (content will be generated for all languages)", if (isResubmit) " (resubmit)" else "", id)
                    triggerPipelineExecutor.execute {
                        try {
                            storyProcessingService.processSync(id)
                            log.info("PIPELINE >>> Submit-for-review pipeline completed for story id={}", id)
                        } catch (e: Exception) {
                            log.error("PIPELINE >>> Submit-for-review pipeline failed for story id={}: {}", id, e.message, e)
                        }
                    }
                } else {
                    log.info("PIPELINE >>> Submit for review: storyId={} saved (audio-after-approval=true; use Story to Speech after approval to generate audio)", id)
                }
            }
            return ResponseEntity.ok(
                story.toResponse(
                    coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                    coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
                )
            )
        }
        log.warn("Admin update curated story id={} not found", id)
        return ResponseEntity.notFound().build<com.tamixa.api.admin.dto.LibraryStoryResponse>()
    }

    @PostMapping("/stories/{id}/retry")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun retryLibraryStory(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        log.info("PIPELINE >>> RECEIVED retry storyId={} (scheduling background)", id)
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
    @PostMapping("/stories/{id}/trigger-pipeline")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun triggerLibraryStoryPipeline(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
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
     * Request changes: send story back to author with feedback.
     * Valid when story is in review queue (PUBLISHED, PROCESSING, or READY; narrationApprovedAt=null).
     */
    @PostMapping("/stories/{id}/request-changes")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun requestChanges(@PathVariable id: Long, @Valid @RequestBody body: ReviewNotesRequest?): ResponseEntity<*> {
        val notes = body?.notes?.trim()?.takeIf { it.isNotBlank() }
        return if (storyLibraryService.requestChanges(id, notes)) {
            ResponseEntity.ok(mapOf("message" to "Story sent back for changes", "status" to "CHANGES_REQUESTED"))
        } else {
            ResponseEntity.badRequest().body(mapOf("message" to "Story not found or not in review queue"))
        }
    }

    /**
     * Reject curated story (final rejection in Story for review flow).
     * Valid when story is in review queue (PUBLISHED, PROCESSING, or READY; narrationApprovedAt=null).
     */
    @PostMapping("/stories/{id}/review-reject")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun rejectLibraryStory(@PathVariable id: Long, @Valid @RequestBody body: ReviewNotesRequest?): ResponseEntity<*> {
        val notes = body?.notes?.trim()?.takeIf { it.isNotBlank() }
        return if (storyLibraryService.reject(id, notes)) {
            ResponseEntity.ok(mapOf("message" to "Story rejected", "status" to "REJECTED"))
        } else {
            ResponseEntity.badRequest().body(mapOf("message" to "Story not found or not in review queue"))
        }
    }

    /**
     * Approve narrated story for final delivery (human verification).
     * Call after reviewing content in Story for review. Does NOT trigger pipeline—content/audio
     * already exists from Submit for review. Only sets narration_approved_at so the story appears on the app.
     */
    @PostMapping("/stories/{id}/approve-narration")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun approveLibraryStoryNarration(@PathVariable id: Long): ResponseEntity<*> {
        val approved = storyLibraryService.approveNarration(id)
        return if (approved) {
            log.info("Narration approved for story id={}; story is now published and visible on the app (no pipeline trigger)", id)
            ResponseEntity.ok(mapOf("message" to "Narration approved for delivery. Story is now visible on the app."))
        } else {
            ResponseEntity.badRequest().body(mapOf("message" to "Story not found or not in review queue"))
        }
    }

    /**
     * Clear library_stories.audio_file_url so serving uses story_narration_audio only.
     * Use when migrating from legacy stored URLs to narration pipeline.
     */
    @DeleteMapping("/stories/legacy-audio")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun clearLegacyAudio(): ResponseEntity<Map<String, Any>> {
        val (deletedAudio, clearedUrls, s3ObjectsDeleted) = storyLibraryService.clearLegacyAudio()
        return ResponseEntity.ok(
            mapOf(
                "message" to "Legacy audio removed",
                "storyAudioDeleted" to deletedAudio,
                "libraryAudioUrlsCleared" to clearedUrls,
                "s3ObjectsDeleted" to s3ObjectsDeleted
            )
        )
    }

    /**
     * Nuclear cleanup: delete ALL narration audio, S3 objects, clear audio_file_url, flush Redis TTS cache.
     * Use when cache/audio is corrupted or stale. Restart backend and admin after. Pipeline will regenerate on next approval.
     */
    @DeleteMapping("/stories/clear-all-audio-and-cache")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun clearAllAudioAndCache(): ResponseEntity<Map<String, Any>> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        val result = mutableMapOf<String, Any>()
        result.putAll(storyLibraryService.clearAllNarrationAndAudio())
        ttsMetadataCache?.flushAll()
        result["redisTtsCacheFlushed"] = 1
        result["message"] = "All narration audio, legacy audio, S3 objects, and TTS cache cleared. Restart backend/admin; pipeline will regenerate on next approval."
        log.info("Admin clear-all-audio-and-cache: {}", result)
        adminService.recordAdminAuditAction(adminEmail, "clear_all_audio_and_cache", "story", null, result.toString())
        return ResponseEntity.ok(result as Map<String, Any>)
    }

    /**
     * Regenerate narration audio: removes old audio, then TTS-only (no translate/rewrite).
     * Optional languages: when provided, only those languages are regenerated (e.g. truncation-flagged).
     * Use when audio sounds flat, truncated, or to refresh TTS; existing content and script are reused.
     */
    @PostMapping("/stories/{id}/regenerate-narration")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun regenerateLibraryStoryNarration(
        @PathVariable id: Long,
        @Valid @RequestBody(required = false) request: RegenerateNarrationRequest?
    ): ResponseEntity<Map<String, String>> {
        val languages = request?.languages?.filter { it.isNotBlank() }?.ifEmpty { null }
        triggerPipelineExecutor.execute {
            try {
                storyProcessingService.regenerateNarration(id, languages)
                log.info("Regenerate pipeline completed for story id={} languages={}", id, languages ?: "all")
            } catch (e: Exception) {
                log.error("Regenerate pipeline failed for story id={}: {}", id, e.message, e)
            }
        }
        val scope = if (languages.isNullOrEmpty()) "all languages" else "languages ${languages.joinToString(",")}"
        return ResponseEntity.ok(mapOf("message" to "Regenerate narration triggered for curated story $id ($scope)"))
    }

    /**
     * Republish: clear audio for selected languages and reprocess. Empty languages = all.
     */
    @PostMapping("/stories/{id}/republish")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun republishLibraryStory(
        @PathVariable id: Long,
        @Valid @RequestBody(required = false) request: RepublishRequest?
    ): ResponseEntity<Map<String, String>> {
        val languages = request?.languages?.filter { it.isNotBlank() } ?: emptyList()
        log.info("Republish requested storyId={} languages={}", id, languages)
        triggerPipelineExecutor.execute {
            try {
                storyProcessingService.republishLanguages(id, languages)
                log.info("Republish pipeline completed for story id={}", id)
            } catch (e: Exception) {
                log.error("Republish pipeline failed for story id={}: {}", id, e.message, e)
            }
        }
        val scope = if (languages.isEmpty()) "all languages" else "languages ${languages.joinToString()}"
        return ResponseEntity.ok(mapOf("message" to "Republish triggered for curated story $id ($scope)"))
    }

    @PutMapping("/stories/bulk-publish")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun bulkPublish(@Valid @RequestBody request: BulkPublishRequest): ResponseEntity<Map<String, Any>> {
        val count = storyLibraryService.bulkPublish(request.ids)
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

    @PutMapping("/stories/bulk-category")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun bulkUpdateCategory(@Valid @RequestBody request: BulkUpdateCategoryRequest): ResponseEntity<Map<String, Any>> {
        val count = storyLibraryService.bulkUpdateCategory(request.ids, request.theme)
        return ResponseEntity.ok(mapOf("updated" to count, "theme" to request.theme))
    }

    @DeleteMapping("/stories/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun deleteCuratedStory(@PathVariable id: Long): ResponseEntity<*> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        return if (storyLibraryService.deleteById(id)) {
            adminService.recordAdminAuditAction(adminEmail, "delete_story", "story", id.toString(), "Story deleted")
            ResponseEntity.noContent().build<Unit>()
        } else {
            ResponseEntity.notFound().build<Unit>()
        }
    }

    @DeleteMapping("/stories/bulk")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    fun bulkDeleteLibraryStories(@Valid @RequestBody request: BulkDeleteRequest): ResponseEntity<Map<String, Any>> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        val deleted = storyLibraryService.deleteByIds(request.ids)
        if (deleted > 0) {
            adminService.recordAdminAuditAction(adminEmail, "bulk_delete_stories", "story", null, "Deleted ${request.ids.size} stories: ${request.ids}")
        }
        return ResponseEntity.ok(mapOf("deleted" to deleted, "ids" to request.ids))
    }

    @PostMapping("/stories")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_STORIES')")
    fun createLibraryStory(@Valid @RequestBody request: CreateLibraryStoryRequest): ResponseEntity<*> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "system"
        val story = storyLibraryService.create(
            title = request.title,
            content = request.content,
            theme = request.theme,
            category = request.category,
            language = request.language.ifBlank { "ta" },
            age = request.age,
            childName = request.childName.ifBlank { "Child" },
            moral = request.moral,
            audioFileUrl = request.audioFileUrl,
            status = request.status.ifBlank { "DRAFT" },
            coverImageUrl = coverImageUrlResolver.normalizeForStorage(request.coverImageUrl) ?: request.coverImageUrl,
            emotionMode = request.emotionMode,
            storyOwner = adminEmail,
            convertPromptUsed = null
        )
        if (story.status == LibraryStoryStatus.PUBLISHED && appProperties.translationPipeline.pipelineOnSubmitOnly) {
            // #region agent log
            debugLog(
                runId = "create-first-submit",
                hypothesisId = "H12",
                location = "AdminController.kt:createLibraryStory:pipeline-trigger",
                message = "create publish triggering pipeline",
                data = mapOf("storyId" to story.id, "status" to story.status)
            )
            // #endregion
            log.info("PIPELINE >>> Create publish: triggering pipeline for storyId={} (content will be generated for all languages)", story.id)
            triggerPipelineExecutor.execute {
                try {
                    // #region agent log
                    debugLog(
                        runId = "create-first-submit",
                        hypothesisId = "H14",
                        location = "AdminController.kt:createLibraryStory:pipeline-task-start",
                        message = "create publish pipeline task started",
                        data = mapOf("storyId" to story.id)
                    )
                    // #endregion
                    storyProcessingService.processSync(story.id)
                    log.info("PIPELINE >>> Create-publish pipeline completed for story id={}", story.id)
                } catch (e: Exception) {
                    log.error("PIPELINE >>> Create-publish pipeline failed for story id={}: {}", story.id, e.message, e)
                }
            }
        }
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(
            story.toResponse(
                coverImageUrlResolver.resolveCoverPath(story.coverImageUrl),
                coverImageUrlResolver.resolveCoverVideoPath(story.coverVideoUrl)
            )
        )
    }

    /**
     * Bulk-generate library stories using the Tamixa storyteller prompt template.
     * Request:
     * {
     *   "languages": ["ta","en"],
     *   "categories": ["Friendship","Village Life"],
     *   "totalStories": 25,
     *   "publish": false
     * }
     */
    @PostMapping("/stories/bulk-generate")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT')")
    fun bulkGenerateLibraryStories(@Valid @RequestBody request: BulkGenerateStoriesRequest): ResponseEntity<*> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "system"
        val rawLangs = request.languages?.mapNotNull { it.trim().takeIf { it.isNotBlank() } }?.distinct() ?: emptyList()
        // Fallback to Tamil only (never Hindi or other). When languages provided, use all of them so stories are generated in each.
        val languages = if (rawLangs.isEmpty()) listOf("ta") else rawLangs
        val categories = request.categories?.mapNotNull { it.trim().takeIf { it.isNotBlank() } }?.distinct() ?: emptyList()
        val totalStories = (request.totalStories?.coerceIn(1, 25)) ?: 25
        val publish = request.publish ?: false
        val syncJobId = "sync-${System.currentTimeMillis()}"
        if (!bulkJobStore.tryAcquire(syncJobId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf(
                "message" to "A bulk story generation is already in progress. Wait for it to finish or use async mode and poll by job ID."
            ))
        }
        return try {
            val result = storyLibraryService.generateBulkStoriesWithTemplatePrompt(
                languages = languages,
                categories = categories,
                totalStories = totalStories,
                publish = publish,
                storyOwner = adminEmail
            )
            val createdIds = (result["created"] as? List<*>)?.mapNotNull {
                (it as? Map<*, *>)?.get("id") as? Number
            }?.map { it.toLong() } ?: emptyList()
            if (publish && appProperties.translationPipeline.pipelineOnSubmitOnly) {
                createdIds.forEach { id ->
                    triggerPipelineExecutor.execute {
                        try {
                            storyProcessingService.processSync(id)
                            log.info("PIPELINE >>> Bulk-generate submit pipeline completed for story id={}", id)
                        } catch (e: Exception) {
                            log.error("PIPELINE >>> Bulk-generate submit pipeline failed for story id={}: {}", id, e.message, e)
                        }
                    }
                }
            }
            val createdCount = (result["createdCount"] as? Number)?.toInt() ?: 0
            val failedCount = (result["failedCount"] as? Number)?.toInt() ?: 0
            adminService.recordAdminAuditAction(
                adminEmail,
                "bulk_generate_stories",
                "story",
                null,
                "requested=$totalStories created=$createdCount failed=$failedCount publish=$publish"
            )
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Invalid bulk generate request")))
        } finally {
            bulkJobStore.release(syncJobId)
        }
    }

    /**
     * Start bulk story generation asynchronously. Returns job ID immediately; poll GET /stories/bulk-generate/jobs/{jobId} for progress and result.
     * One bulk run at a time (sync or async); returns 409 if another is already running.
     */
    @PostMapping("/stories/bulk-generate/async")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT')")
    fun bulkGenerateLibraryStoriesAsync(@Valid @RequestBody request: BulkGenerateStoriesRequest): ResponseEntity<*> {
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "system"
        val rawLangs = request.languages?.mapNotNull { it.trim().takeIf { it.isNotBlank() } }?.distinct() ?: emptyList()
        // Fallback to Tamil only (never Hindi or other). When languages provided, use all of them so stories are generated in each.
        val languages = if (rawLangs.isEmpty()) listOf("ta") else rawLangs
        val categories = request.categories?.mapNotNull { it.trim().takeIf { it.isNotBlank() } }?.distinct() ?: emptyList()
        val totalStories = (request.totalStories?.coerceIn(1, 25)) ?: 25
        val publish = request.publish ?: false
        val jobId = bulkJobStore.create(totalStories, publish)
        if (!bulkJobStore.tryAcquire(jobId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf(
                "message" to "A bulk story generation is already in progress.",
                "runningJobId" to (bulkJobStore.getRunningJobId() ?: "")
            ))
        }
        bulkJobStore.setRunning(jobId)
        triggerPipelineExecutor.execute {
            try {
                val result = storyLibraryService.generateBulkStoriesWithTemplatePrompt(
                    languages = languages,
                    categories = categories,
                    totalStories = totalStories,
                    publish = publish,
                    storyOwner = adminEmail,
                    progressCallback = { current, total, createdCount, failedCount, created, failed ->
                        bulkJobStore.updateProgress(jobId, current, createdCount, failedCount, created, failed)
                    }
                )
                val createdIds = (result["created"] as? List<*>)?.mapNotNull {
                    (it as? Map<*, *>)?.get("id") as? Number
                }?.map { it.toLong() } ?: emptyList()
                if (publish && appProperties.translationPipeline.pipelineOnSubmitOnly) {
                    createdIds.forEach { id ->
                        triggerPipelineExecutor.execute {
                            try {
                                storyProcessingService.processSync(id)
                                log.info("PIPELINE >>> Bulk-generate submit pipeline completed for story id={}", id)
                            } catch (e: Exception) {
                                log.error("PIPELINE >>> Bulk-generate submit pipeline failed for story id={}: {}", id, e.message, e)
                            }
                        }
                    }
                }
                val createdCount = (result["createdCount"] as? Number)?.toInt() ?: 0
                val failedCount = (result["failedCount"] as? Number)?.toInt() ?: 0
                adminService.recordAdminAuditAction(
                    adminEmail,
                    "bulk_generate_stories_async",
                    "story",
                    null,
                    "jobId=$jobId requested=$totalStories created=$createdCount failed=$failedCount publish=$publish"
                )
                bulkJobStore.complete(jobId, result)
            } catch (e: Exception) {
                log.error("Bulk async job failed jobId={}: {}", jobId, e.message, e)
                bulkJobStore.fail(jobId, e.message ?: "Bulk generation failed")
            }
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapOf("jobId" to jobId))
    }

    /**
     * Poll status and result of an async bulk story generation job.
     * Returns 404 if job unknown; 200 with status, progress (current/total), and result when completed.
     */
    @GetMapping("/stories/bulk-generate/jobs/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER','REVENUE_ANALYST','SUPPORT')")
    fun getBulkGenerateJobStatus(@PathVariable jobId: String): ResponseEntity<*> {
        val state = bulkJobStore.get(jobId) ?: return ResponseEntity.notFound().build<Unit>()
        val body = mutableMapOf<String, Any?>(
            "jobId" to state.jobId,
            "status" to state.status.name,
            "requestedTotal" to state.requestedTotal,
            "currentIndex" to state.currentIndex,
            "createdCount" to state.createdCount,
            "failedCount" to state.failedCount,
            "publish" to state.publish
        )
        body["progress"] = mapOf("current" to state.currentIndex, "total" to state.requestedTotal)
        if (state.errorMessage != null) body["errorMessage"] = state.errorMessage
        if (state.status == BulkJobStatus.COMPLETED || state.status == BulkJobStatus.FAILED) {
            body["result"] = mapOf(
                "requested" to state.requestedTotal,
                "createdCount" to state.createdCount,
                "failedCount" to state.failedCount,
                "publish" to state.publish,
                "created" to state.created,
                "failed" to state.failed
            )
            state.startedAt?.let { body["startedAt"] = it.toString() }
            state.completedAt?.let { body["completedAt"] = it.toString() }
        }
        return ResponseEntity.ok(body)
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
