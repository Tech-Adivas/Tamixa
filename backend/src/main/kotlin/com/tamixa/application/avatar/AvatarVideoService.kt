package com.tamixa.application.avatar

import com.tamixa.application.port.ParentAvatarRepositoryPort
import com.tamixa.application.port.StoryAvatarVideoRepositoryPort
import com.tamixa.application.port.StoryAvatarVideoStoragePort
import com.tamixa.application.stream.AudioStreamService
import com.tamixa.application.stream.NarrationScriptService
import com.tamixa.domain.AvatarVideoStatus
import com.tamixa.domain.StoryAvatarVideo
import com.tamixa.infrastructure.avatar.DidAvatarVideoClient
import com.tamixa.infrastructure.avatar.GooeyLipSyncClient
import com.tamixa.infrastructure.avatar.HeyGenAvatarVideoClient
import com.tamixa.infrastructure.avatar.HeyGenCreateVideoException
import com.tamixa.infrastructure.avatar.ReplicateSadTalkerClient
import com.tamixa.infrastructure.cdn.S3SignedUrlGenerator
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.observability.AiApiMetrics
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.dao.DataIntegrityViolationException
import java.net.URL
import java.time.Instant

@Service
class AvatarVideoService(
    private val storyAvatarVideoRepository: StoryAvatarVideoRepositoryPort,
    private val parentAvatarRepository: ParentAvatarRepositoryPort,
    private val audioStreamService: AudioStreamService,
    private val narrationScriptService: NarrationScriptService,
    private val appProperties: AppProperties,
    heyGenClientProvider: ObjectProvider<HeyGenAvatarVideoClient>,
    replicateClientProvider: ObjectProvider<ReplicateSadTalkerClient>,
    gooeyClientProvider: ObjectProvider<GooeyLipSyncClient>,
    didClientProvider: ObjectProvider<DidAvatarVideoClient>,
    private val storyAvatarVideoStorageProvider: ObjectProvider<StoryAvatarVideoStoragePort>,
    @Autowired(required = false) private val s3SignedUrlGenerator: S3SignedUrlGenerator?,
    @Autowired(required = false) private val aiApiMetrics: AiApiMetrics?,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val heyGenClient: HeyGenAvatarVideoClient? = heyGenClientProvider.getIfAvailable()
    private val replicateClient: ReplicateSadTalkerClient? = replicateClientProvider.getIfAvailable()
    private val gooeyClient: GooeyLipSyncClient? = gooeyClientProvider.getIfAvailable()
    private val didClient: DidAvatarVideoClient? = didClientProvider.getIfAvailable()
    private val videoStorage: StoryAvatarVideoStoragePort? = storyAvatarVideoStorageProvider.getIfAvailable()

    /**
     * Get signed URL for avatar video if READY. Triggers async generation if not exists.
     * Avatar video is always story-specific: one record per (storyId, storySource, parentId, language, voiceProfile);
     * storage path is avatar_videos/{storyId}_{parentId}_{language}_{voice}.mp4. Each story has its own talking-head video.
     *
     * @param storyId the specific story; avatar is generated from this story's audio only
     */
    fun getAvatarVideoUrl(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String
    ): String? {
        if (!appProperties.avatarVideo.enabled) return null
        val existing = storyAvatarVideoRepository.findByStoryAndParent(
            storyId, storySource, parentId, language, voiceProfile
        )
        when (existing?.status) {
            AvatarVideoStatus.READY -> {
                existing.storagePath?.let { path -> return buildSignedUrl(path) }
            }
            AvatarVideoStatus.PENDING, AvatarVideoStatus.PROCESSING -> return null
            AvatarVideoStatus.FAILED -> {
                storyAvatarVideoRepository.delete(existing)
                log.info("Avatar video: cleared FAILED record for retry storyId={} parentId={}", storyId, parentId)
                // Continue to same create-and-trigger logic as null case below
            }
            null -> { }
        }
        if (existing?.status == AvatarVideoStatus.FAILED || existing == null) {
            // Create PENDING record and trigger async generation (or retry after FAILED)
            ensureParentHasAvatar(parentId) ?: return null
            ensureAudioExists(storyId, storySource, language, voiceProfile, parentId) ?: return null
            val record = StoryAvatarVideo(
                id = 0,
                storyId = storyId,
                storySource = storySource,
                parentId = parentId,
                language = language,
                voiceProfile = voiceProfile,
                storagePath = null,
                status = AvatarVideoStatus.PENDING,
                replicatePredictionId = null,
                errorMessage = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
            try {
                storyAvatarVideoRepository.save(record)
            } catch (e: DataIntegrityViolationException) {
                log.debug("Avatar video PENDING already exists (race), skipping duplicate trigger storyId={} parentId={}", storyId, parentId)
                return null
            }
            triggerGenerationAsync(storyId, storySource, parentId, language, voiceProfile)
            return null
        }
        return null
    }

    /**
     * Deletes the avatar video record and stored file for the given story+parent+language+voice.
     * Used by admin to allow regeneration. Returns true if a record was found and deleted.
     */
    @Transactional
    fun deleteAvatarVideo(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String
    ): Boolean {
        if (!appProperties.avatarVideo.enabled) return false
        val existing = storyAvatarVideoRepository.findByStoryAndParent(
            storyId, storySource, parentId, language, voiceProfile
        ) ?: return false
        existing.storagePath?.let { path ->
            try {
                videoStorage?.delete(path)
            } catch (e: Exception) {
                log.warn("Avatar video: failed to delete storage file {}: {}", path, e.message)
            }
        }
        storyAvatarVideoRepository.delete(existing)
        log.info("Avatar video deleted for regeneration storyId={} parentId={} lang={} voice={}", storyId, parentId, language, voiceProfile)
        return true
    }

    /**
     * Returns which lip-sync provider is active. HeyGen is primary; others are fallbacks when HeyGen is not configured.
     */
    fun getAvatarVideoProviderInfo(): String? {
        if (!appProperties.avatarVideo.enabled) return null
        return when {
            heyGenClient != null -> "heygen"
            replicateClient != null -> "sadtalker (fallback; primary is HeyGen)"
            didClient != null -> "d-id (fallback; primary is HeyGen)"
            gooeyClient != null -> "gooey (fallback; primary is HeyGen)"
            else -> null
        }
    }

    /**
     * Returns current avatar video status and error message (when FAILED) for admin/UI.
     */
    fun getAvatarVideoStatus(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String
    ): Pair<AvatarVideoStatus?, String?>? {
        if (!appProperties.avatarVideo.enabled) return null
        val existing = storyAvatarVideoRepository.findByStoryAndParent(
            storyId, storySource, parentId, language, voiceProfile
        ) ?: return null
        return Pair(existing.status, existing.errorMessage)
    }

    /**
     * Triggers async avatar video generation for this story only. Uses this story's audio URL
     * (getAudioUrlForReplicate(storyId, ...)) and stores the result under avatar_videos/{storyId}_....
     */
    @Async
    fun triggerGenerationAsync(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String
    ) {
        val provider = when {
            heyGenClient != null -> "heygen"
            replicateClient != null -> "sadtalker (fallback)"
            didClient != null -> "d-id (fallback)"
            gooeyClient != null -> "gooey (fallback)"
            else -> "none"
        }
        log.warn(
            "Avatar video triggerGenerationAsync started storyId={} parentId={} lang={} provider={} videoStorage={} (primary=HeyGen; use AVATAR_VIDEO_PROVIDER=heygen+HEYGEN_API_KEY for primary)",
            storyId, parentId, language, provider, videoStorage != null
        )
        try {
            if (videoStorage == null) {
                log.warn("Avatar video: storage not configured (S3 required for avatar video)")
                markFailed(storyId, storySource, parentId, language, voiceProfile, "Avatar video storage not configured (S3 required)")
                return
            }
            val audioUrl = getAudioUrlForReplicate(storyId, storySource, language, voiceProfile, parentId) ?: run {
                log.warn("Avatar video: no public audio URL for storyId={} lang={} voice={} parentId={}", storyId, language, voiceProfile, parentId)
                markFailed(storyId, storySource, parentId, language, voiceProfile, "No public audio URL (use S3 and ensure narration exists)")
                return
            }
            // HeyGen is primary; Replicate, D-ID, Gooey are fallbacks when HeyGen is not configured
            when {
                heyGenClient != null -> {
                    aiApiMetrics?.recordHeyGenAvatar()
                    log.info("Avatar video: starting HeyGen flow (primary) storyId={} parentId={} lang={}", storyId, parentId, language)
                    runHeyGenFlow(storyId, storySource, parentId, language, voiceProfile, audioUrl)
                }
                replicateClient != null -> {
                    aiApiMetrics?.recordReplicateAvatar()
                    log.info("Avatar video: using fallback Replicate (primary is HeyGen; set AVATAR_VIDEO_PROVIDER=heygen and HEYGEN_API_KEY to use HeyGen). storyId={} parentId={} lang={}", storyId, parentId, language)
                    runReplicateFlow(storyId, storySource, parentId, language, voiceProfile, audioUrl)
                }
                didClient != null -> {
                    aiApiMetrics?.recordDidAvatar()
                    log.info("Avatar video: using fallback D-ID (primary is HeyGen; set AVATAR_VIDEO_PROVIDER=heygen and HEYGEN_API_KEY to use HeyGen). storyId={} parentId={} lang={}", storyId, parentId, language)
                    runDidFlow(storyId, storySource, parentId, language, voiceProfile, audioUrl)
                }
                gooeyClient != null -> {
                    val textPrompt = narrationScriptService.getNarrationScript(storyId, language)?.takeIf { it.isNotBlank() }
                    if (textPrompt == null || textPrompt.isBlank()) {
                        log.warn("Avatar video: no narration script for Gooey storyId={} lang={}", storyId, language)
                        markFailed(storyId, storySource, parentId, language, voiceProfile, "No story script for avatar video (ensure story has narration)")
                        return
                    }
                    aiApiMetrics?.recordGooeyAvatar()
                    log.info("Avatar video: using fallback Gooey (primary is HeyGen; set AVATAR_VIDEO_PROVIDER=heygen and HEYGEN_API_KEY to use HeyGen). storyId={} parentId={} lang={}", storyId, parentId, language)
                    runGooeyFlow(storyId, storySource, parentId, language, voiceProfile, audioUrl, textPrompt)
                }
                else -> {
                    val msg = (
                        "No avatar video provider. Primary is HeyGen: set AVATAR_VIDEO_ENABLED=true, AVATAR_VIDEO_PROVIDER=heygen, HEYGEN_API_KEY=your_key. " +
                            "Fallbacks (when HeyGen not configured): sadtalker (REPLICATE_API_TOKEN), d-id (DID_API_KEY), gooey (GOOEY_API_KEY+GOOEY_RECIPE_ID). Restart the backend after changing .env."
                        )
                    log.warn("Avatar video: {}", msg)
                    markFailed(storyId, storySource, parentId, language, voiceProfile, msg)
                }
            }
        } catch (e: Exception) {
            log.error("Avatar video generation failed storyId={} parentId={}: {}", storyId, parentId, e.message, e)
            markFailed(storyId, storySource, parentId, language, voiceProfile, e.message ?: "Unexpected error")
        }
    }

    private fun runHeyGenFlow(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        audioUrl: String
    ) {
        val (imageBytes, contentType) = getAvatarImageBytes(parentId) ?: run {
            markFailed(storyId, storySource, parentId, language, voiceProfile, "No avatar image")
            return
        }
        val talkingPhotoId: String
        try {
            talkingPhotoId = heyGenClient!!.uploadTalkingPhoto(imageBytes, contentType)
                ?: run {
                    markFailed(storyId, storySource, parentId, language, voiceProfile, "HeyGen talking photo upload failed")
                    return
                }
        } catch (e: HeyGenCreateVideoException) {
            markFailed(storyId, storySource, parentId, language, voiceProfile, e.message ?: "HeyGen talking photo upload failed")
            return
        }
        try {
            val videoId = heyGenClient.createVideo(talkingPhotoId, audioUrl)
            updatePredictionId(storyId, storySource, parentId, language, voiceProfile, videoId)
            pollAndCompleteHeyGen(storyId, storySource, parentId, language, voiceProfile, videoId)
        } catch (e: HeyGenCreateVideoException) {
            markFailed(storyId, storySource, parentId, language, voiceProfile, e.message ?: "HeyGen create video failed")
        }
    }

    private fun runReplicateFlow(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        audioUrl: String
    ) {
        val avatarUrl = getAvatarImageUrlForReplicate(parentId) ?: run {
            log.warn("Avatar video: no avatar image URL for parentId={}", parentId)
            markFailed(storyId, storySource, parentId, language, voiceProfile, "No avatar image")
            return
        }
        log.info("Avatar video: calling Replicate createPrediction storyId={} parentId={} avatarUrlLen={} audioUrlLen={}", storyId, parentId, avatarUrl.length, audioUrl.length)
        val predictionId = replicateClient!!.createPrediction(avatarUrl, audioUrl) ?: run {
            log.warn("Avatar video: Replicate createPrediction returned null storyId={} parentId={}", storyId, parentId)
            markFailed(storyId, storySource, parentId, language, voiceProfile, "Replicate create failed (check REPLICATE_API_TOKEN and logs)")
            return
        }
        updatePredictionId(storyId, storySource, parentId, language, voiceProfile, predictionId)
        pollAndCompleteReplicate(storyId, storySource, parentId, language, voiceProfile, predictionId)
    }

    private fun runDidFlow(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        audioUrl: String
    ) {
        val avatarUrl = getAvatarImageUrlForReplicate(parentId) ?: run {
            log.warn("Avatar video: no avatar image URL for parentId={}", parentId)
            markFailed(storyId, storySource, parentId, language, voiceProfile, "No avatar image")
            return
        }
        log.info("Avatar video: calling D-ID createTalk storyId={} parentId={}", storyId, parentId)
        val talkId = didClient!!.createTalk(avatarUrl, audioUrl) ?: run {
            log.warn("Avatar video: D-ID createTalk returned null storyId={} parentId={}", storyId, parentId)
            markFailed(storyId, storySource, parentId, language, voiceProfile, "D-ID create talk failed (check DID_API_KEY)")
            return
        }
        updatePredictionId(storyId, storySource, parentId, language, voiceProfile, talkId)
        pollAndCompleteDid(storyId, storySource, parentId, language, voiceProfile, talkId)
    }

    private fun runGooeyFlow(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        audioUrl: String,
        textPrompt: String
    ) {
        val avatarUrl = getAvatarImageUrlForReplicate(parentId) ?: run {
            log.warn("Avatar video: no avatar image URL for parentId={}", parentId)
            markFailed(storyId, storySource, parentId, language, voiceProfile, "No avatar image")
            return
        }
        log.info("Avatar video: calling Gooey createJob storyId={} parentId={}", storyId, parentId)
        val jobId = gooeyClient!!.createJob(avatarUrl, audioUrl, textPrompt) ?: run {
            log.warn("Avatar video: Gooey createJob returned null storyId={} parentId={}", storyId, parentId)
            markFailed(storyId, storySource, parentId, language, voiceProfile, "Gooey create failed (check GOOEY_API_KEY and GOOEY_RECIPE_ID)")
            return
        }
        updatePredictionId(storyId, storySource, parentId, language, voiceProfile, jobId)
        pollAndCompleteGooey(storyId, storySource, parentId, language, voiceProfile, jobId)
    }

    private fun getAvatarImageBytes(parentId: Long): Pair<ByteArray, String>? {
        val avatar = parentAvatarRepository.findByParentId(parentId) ?: return null
        val url = buildSignedUrl(avatar.storagePath) ?: return null
        return try {
            val bytes = URL(url).openStream().readBytes()
            val contentType = if (avatar.contentType.contains("png")) "image/png" else "image/jpeg"
            Pair(bytes, contentType)
        } catch (e: Exception) {
            log.warn("Failed to fetch avatar bytes: {}", e.message)
            null
        }
    }

    private fun pollAndCompleteHeyGen(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        videoId: String
    ) {
        val props = appProperties.avatarVideo
        repeat(props.maxPollAttempts) {
            Thread.sleep(props.pollIntervalSeconds * 1000L)
            when (val result = heyGenClient!!.getVideoStatus(videoId)) {
                is HeyGenAvatarVideoClient.AvatarVideoResult.Succeeded -> {
                    downloadAndStore(
                        storyId, storySource, parentId, language, voiceProfile,
                        result.videoUrl
                    )
                    return
                }
                is HeyGenAvatarVideoClient.AvatarVideoResult.Error -> {
                    markFailed(storyId, storySource, parentId, language, voiceProfile, result.message)
                    return
                }
                else -> { /* Pending, continue */ }
            }
        }
        markFailed(storyId, storySource, parentId, language, voiceProfile, "Timeout waiting for HeyGen")
    }

    private fun pollAndCompleteDid(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        talkId: String
    ) {
        val props = appProperties.avatarVideo
        repeat(props.maxPollAttempts) {
            Thread.sleep(props.pollIntervalSeconds * 1000L)
            when (val result = didClient!!.getTalkStatus(talkId)) {
                is DidAvatarVideoClient.DidTalkResult.Done -> {
                    downloadAndStore(
                        storyId, storySource, parentId, language, voiceProfile,
                        result.resultUrl
                    )
                    return
                }
                is DidAvatarVideoClient.DidTalkResult.Error -> {
                    markFailed(storyId, storySource, parentId, language, voiceProfile, result.message)
                    return
                }
                else -> { /* Pending, continue */ }
            }
        }
        markFailed(storyId, storySource, parentId, language, voiceProfile, "Timeout waiting for D-ID (typical 10–30s per talk)")
    }

    private fun pollAndCompleteReplicate(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        predictionId: String
    ) {
        val props = appProperties.avatarVideo
        val maxWaitSec = props.maxPollAttempts * props.pollIntervalSeconds
        log.info("Avatar video: polling Replicate predictionId={} (max {}s, interval {}s)", predictionId.take(20), maxWaitSec, props.pollIntervalSeconds)
        repeat(props.maxPollAttempts) {
            Thread.sleep(props.pollIntervalSeconds * 1000L)
            when (val result = replicateClient!!.getPrediction(predictionId)) {
                is ReplicateSadTalkerClient.ReplicatePredictionResult.Succeeded -> {
                    downloadAndStore(
                        storyId, storySource, parentId, language, voiceProfile,
                        result.videoUrl
                    )
                    return
                }
                is ReplicateSadTalkerClient.ReplicatePredictionResult.Error -> {
                    markFailed(storyId, storySource, parentId, language, voiceProfile, result.message)
                    return
                }
                else -> { /* Pending, continue */ }
            }
        }
        log.warn("Avatar video: Replicate polling timed out after {}s storyId={} parentId={} predictionId={}", maxWaitSec, storyId, parentId, predictionId.take(20))
        markFailed(storyId, storySource, parentId, language, voiceProfile, "Timeout waiting for Replicate (${maxWaitSec}s). SadTalker can take 2–3 min on cold start; increase AVATAR_VIDEO_MAX_POLL if needed.")
    }

    private fun pollAndCompleteGooey(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        jobId: String
    ) {
        val props = appProperties.avatarVideo
        val maxWaitSec = props.maxPollAttempts * props.pollIntervalSeconds
        log.info("Avatar video: polling Gooey jobId={} (max {}s)", jobId.take(50), maxWaitSec)
        repeat(props.maxPollAttempts) {
            Thread.sleep(props.pollIntervalSeconds * 1000L)
            when (val result = gooeyClient!!.getJobStatus(jobId)) {
                is GooeyLipSyncClient.GooeyJobResult.Succeeded -> {
                    downloadAndStore(
                        storyId, storySource, parentId, language, voiceProfile,
                        result.videoUrl
                    )
                    return
                }
                is GooeyLipSyncClient.GooeyJobResult.Error -> {
                    markFailed(storyId, storySource, parentId, language, voiceProfile, result.message)
                    return
                }
                else -> { /* Pending, continue */ }
            }
        }
        markFailed(storyId, storySource, parentId, language, voiceProfile, "Timeout waiting for Gooey (${maxWaitSec}s). Increase AVATAR_VIDEO_MAX_POLL if needed.")
    }

    private fun downloadAndStore(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        videoUrl: String
    ) {
        try {
            val conn = URL(videoUrl).openConnection()
            conn.connectTimeout = appProperties.avatarVideo.connectTimeoutMs.toInt()
            conn.readTimeout = appProperties.avatarVideo.readTimeoutMs.toInt()
            val bytes = conn.getInputStream().use { it.readBytes() }
            val storagePath = "avatar_videos/${storyId}_${parentId}_${language}_${voiceProfile.replace(Regex("[^a-zA-Z0-9_-]"), "_")}.mp4"
            videoStorage!!.upload(storagePath, bytes)
            markReady(storyId, storySource, parentId, language, voiceProfile, storagePath)
        } catch (e: Exception) {
            log.error("Avatar video download/store failed: {}", e.message, e)
            markFailed(storyId, storySource, parentId, language, voiceProfile, e.message ?: "Download failed")
        }
    }

    private fun ensureParentHasAvatar(parentId: Long): Boolean =
        parentAvatarRepository.findByParentId(parentId) != null

    private fun ensureAudioExists(
        storyId: Long,
        storySource: String,
        language: String,
        voiceProfile: String,
        parentId: Long?
    ): Boolean {
        val url = when (storySource) {
            "library" -> audioStreamService.getLibraryNarrationStreamUrlForExternalFetch(storyId, language, voiceProfile, parentId)
                ?: audioStreamService.getLibraryStreamUrl(storyId, language, parentId)
            "generated" -> audioStreamService.getGeneratedStreamUrl(storyId, language, parentId)
            else -> null
        }
        return url != null
    }

    private fun getAvatarImageUrlForReplicate(parentId: Long): String? {
        val avatar = parentAvatarRepository.findByParentId(parentId) ?: return null
        return buildSignedUrl(avatar.storagePath) ?: return null
    }

    /**
     * Returns a publicly reachable audio URL for Replicate (signed S3 only; never proxy/localhost).
     */
    private fun getAudioUrlForReplicate(
        storyId: Long,
        storySource: String,
        language: String,
        voiceProfile: String,
        parentId: Long?
    ): String? {
        return when (storySource) {
            "library" -> {
                if (voiceProfile.equals("family", ignoreCase = true) && parentId != null)
                    audioStreamService.getFamilyVoiceStreamUrl(storyId, language, parentId)
                else
                    audioStreamService.getLibraryNarrationStreamUrlForExternalFetch(storyId, language, voiceProfile, parentId)
            }
            "generated" -> audioStreamService.getGeneratedStreamUrl(storyId, language, parentId)?.takeIf { it.startsWith("https://") }
            else -> null
        }
    }

    @Transactional
    private fun updatePredictionId(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        predictionId: String
    ) {
        val v = storyAvatarVideoRepository.findByStoryAndParent(
            storyId, storySource, parentId, language, voiceProfile
        ) ?: return
        storyAvatarVideoRepository.save(v.copy(
            status = AvatarVideoStatus.PROCESSING,
            replicatePredictionId = predictionId,
            updatedAt = Instant.now()
        ))
    }

    @Transactional
    private fun markReady(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        storagePath: String
    ) {
        val v = storyAvatarVideoRepository.findByStoryAndParent(
            storyId, storySource, parentId, language, voiceProfile
        ) ?: return
        storyAvatarVideoRepository.save(v.copy(
            storagePath = storagePath,
            status = AvatarVideoStatus.READY,
            errorMessage = null,
            updatedAt = Instant.now()
        ))
        log.info("Avatar video READY storyId={} parentId={} path={}", storyId, parentId, storagePath)
    }

    @Transactional
    private fun markFailed(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        error: String
    ) {
        val v = storyAvatarVideoRepository.findByStoryAndParent(
            storyId, storySource, parentId, language, voiceProfile
        ) ?: return
        storyAvatarVideoRepository.save(v.copy(
            status = AvatarVideoStatus.FAILED,
            errorMessage = error.take(500),
            updatedAt = Instant.now()
        ))
        log.warn("Avatar video FAILED storyId={} parentId={}: {}", storyId, parentId, error)
    }

    private fun buildSignedUrl(storagePath: String): String? {
        val expiry = appProperties.cdn.signedUrlExpiryMinutes.coerceIn(5L, 60L)
        return when {
            appProperties.storage.type == "s3" && s3SignedUrlGenerator != null ->
                s3SignedUrlGenerator.signUrl(storagePath, expiry)?.toString()
            else -> null
        }
    }
}
