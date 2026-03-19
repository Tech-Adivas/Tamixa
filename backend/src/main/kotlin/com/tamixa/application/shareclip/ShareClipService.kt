package com.tamixa.application.shareclip

import com.tamixa.application.analytics.StoryAnalyticsEventType
import com.tamixa.application.analytics.StoryAnalyticsService
import com.tamixa.application.port.ShareableClip
import com.tamixa.application.port.ShareableClipRepositoryPort
import com.tamixa.application.port.ShareClipRenderPort
import com.tamixa.application.port.StoryAvatarVideoStoragePort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.stream.AudioStreamService
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.domain.subscription.UpgradeRequiredException
import com.tamixa.infrastructure.cdn.S3SignedUrlGenerator
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.YearMonth

@Service
class ShareClipService(
    private val shareableClipRepository: ShareableClipRepositoryPort,
    private val subscriptionService: SubscriptionService,
    private val audioStreamService: AudioStreamService,
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val storyRepository: StoryRepositoryPort,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    private val appProperties: AppProperties,
    private val shareClipRenderProvider: ObjectProvider<ShareClipRenderPort>,
    @Autowired(required = false) private val storyAvatarVideoStorage: StoryAvatarVideoStoragePort?,
    @Autowired(required = false) private val s3SignedUrlGenerator: S3SignedUrlGenerator?,
    @Autowired(required = false) private val storyAnalyticsService: StoryAnalyticsService?
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val shareClipRender: ShareClipRenderPort? = shareClipRenderProvider.getIfAvailable()

    fun isShareClipEnabled(): Boolean = appProperties.shareClip.enabled

    /**
     * Request clip generation. Premium+ only. Returns clip with PENDING status; async job processes.
     */
    @Transactional
    fun requestClip(
        parentId: Long,
        storyId: Long,
        storySource: String,
        language: String,
        voiceProfile: String,
        startSeconds: Int,
        durationSeconds: Int,
        format: String
    ): ShareableClip {
        if (!appProperties.shareClip.enabled) {
            throw UpgradeRequiredException("Share clips are not enabled")
        }
        val sub = subscriptionService.getOrCreateSubscription(parentId)
        if (!sub.isEntitledToShareClips()) {
            throw UpgradeRequiredException("Premium subscription required for share clips")
        }
        val limit = appProperties.shareClip.clipsPerMonthPremiumPlus
        val since = YearMonth.now().atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        val count = shareableClipRepository.countByParentIdSince(parentId, since)
        if (count >= limit) {
            throw UpgradeRequiredException("Share clip quota exceeded. Limit: $limit per month.")
        }
        val validFormat = if (format in listOf("9:16", "1:1")) format else "9:16"
        val validStart = startSeconds.coerceIn(0, 3600)
        val validDuration = durationSeconds.coerceIn(15, 60)

        val clip = ShareableClip(
            id = 0,
            parentId = parentId,
            storyId = storyId,
            storySource = storySource,
            language = language,
            voiceProfile = voiceProfile,
            startSeconds = validStart,
            durationSeconds = validDuration,
            format = validFormat,
            storagePath = null,
            status = "PENDING",
            processingJobId = null,
            errorMessage = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val saved = shareableClipRepository.save(clip)
        triggerRenderAsync(saved.id)
        storyAnalyticsService?.trackEvent(
            parentId = parentId,
            storyId = storyId,
            storySource = storySource,
            language = language,
            eventType = StoryAnalyticsEventType.SHARE_CLIP_REQUESTED,
            playbackPositionSeconds = 0
        )
        return saved
    }

    fun getClipStatus(clipId: Long, parentId: Long): ShareClipStatusResult? {
        val clip = shareableClipRepository.findByIdAndParentId(clipId, parentId) ?: return null
        val downloadUrl = if (clip.status == "READY" && clip.storagePath != null) {
            buildSignedUrl(clip.storagePath!!)
        } else null
        if (downloadUrl != null && clip.downloadAnalyticsEmittedAt == null) {
            if (shareableClipRepository.markDownloadAnalyticsEmitted(clipId)) {
                storyAnalyticsService?.trackEvent(
                    parentId = parentId,
                    storyId = clip.storyId,
                    storySource = clip.storySource,
                    language = clip.language,
                    eventType = StoryAnalyticsEventType.SHARE_CLIP_DOWNLOADED,
                    playbackPositionSeconds = 0
                )
            }
        }
        return ShareClipStatusResult(
            clipId = clip.id,
            status = clip.status,
            downloadUrl = downloadUrl,
            errorMessage = clip.errorMessage
        )
    }

    fun getQuotaRemaining(parentId: Long): ShareClipQuotaResult {
        val sub = subscriptionService.getOrCreateSubscription(parentId)
        if (!sub.isEntitledToShareClips()) {
            return ShareClipQuotaResult(remaining = 0, limit = 0, entitled = false)
        }
        val limit = appProperties.shareClip.clipsPerMonthPremiumPlus
        val since = YearMonth.now().atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        val used = shareableClipRepository.countByParentIdSince(parentId, since)
        return ShareClipQuotaResult(remaining = (limit - used).coerceAtLeast(0), limit = limit, entitled = true)
    }

    @Async
    fun triggerRenderAsync(clipId: Long) {
        try {
            renderClipInternal(clipId)
        } catch (e: Exception) {
            log.error("Share clip render failed clipId={}: {}", clipId, e.message, e)
            shareableClipRepository.updateStatusAndStorage(clipId, "FAILED", null, e.message?.take(500))
        }
    }

    private fun renderClipInternal(clipId: Long) {
        val clip = shareableClipRepository.findById(clipId) ?: run {
            log.warn("Share clip not found for render clipId={}", clipId)
            return
        }
        shareableClipRepository.updateStatusAndStorage(clipId, "PROCESSING", null, null)

        if (shareClipRender == null) {
            log.warn("Share clip render port not available, marking FAILED clipId={}", clipId)
            shareableClipRepository.updateStatusAndStorage(clipId, "FAILED", null, "Clip rendering not configured")
            return
        }

        val audioUrl = resolveAudioUrl(clip) ?: run {
            shareableClipRepository.updateStatusAndStorage(clipId, "FAILED", null, "Audio not available")
            return
        }
        val coverUrl = resolveCoverUrl(clip) ?: run {
            shareableClipRepository.updateStatusAndStorage(clipId, "FAILED", null, "Cover image not available")
            return
        }

        val videoBytes = shareClipRender.renderClip(
            audioUrl = audioUrl,
            coverImageUrl = coverUrl,
            startSeconds = clip.startSeconds,
            durationSeconds = clip.durationSeconds,
            format = clip.format
        )

        if (videoBytes == null || videoBytes.isEmpty()) {
            shareableClipRepository.updateStatusAndStorage(clipId, "FAILED", null, "Clip render returned empty")
            return
        }

        val storagePath = "share_clips/${clip.parentId}/${clipId}.mp4"
        storyAvatarVideoStorage?.upload(storagePath, videoBytes) ?: run {
            shareableClipRepository.updateStatusAndStorage(clipId, "FAILED", null, "Storage not configured")
            return
        }
        shareableClipRepository.updateStatusAndStorage(clipId, "READY", storagePath, null)
        log.info("Share clip READY clipId={} parentId={} path={}", clipId, clip.parentId, storagePath)
    }

    private fun resolveAudioUrl(clip: ShareableClip): String? {
        return if (clip.storySource == "library") {
            audioStreamService.getLibraryNarrationStreamUrl(clip.storyId, clip.language, clip.voiceProfile, clip.parentId)
                ?: audioStreamService.getLibraryStreamUrl(clip.storyId, clip.language, clip.parentId)
        } else {
            audioStreamService.getGeneratedStreamUrl(clip.storyId, clip.language, clip.parentId)
        }
    }

    private fun resolveCoverUrl(clip: ShareableClip): String? {
        return if (clip.storySource == "library") {
            val story = storyLibraryRepository.findById(clip.storyId) ?: return null
            coverImageUrlResolver.resolveCoverPath(story.coverImageUrl)
        } else {
            val story = storyRepository.findById(clip.storyId) ?: return null
            coverImageUrlResolver.resolveCoverPath(story.coverImageUrl)
                ?: coverImageUrlResolver.resolveCoverUrl(story)
        }?.let { path ->
            if (path.startsWith("http")) path else "${appProperties.audio.publicBaseUrl.trimEnd('/')}/$path"
        }
    }

    private fun buildSignedUrl(storagePath: String): String? =
        s3SignedUrlGenerator?.signUrl(storagePath, 60)?.toString()
}

data class ShareClipStatusResult(
    val clipId: Long,
    val status: String,
    val downloadUrl: String?,
    val errorMessage: String?
)

data class ShareClipQuotaResult(
    val remaining: Int,
    val limit: Int,
    val entitled: Boolean
)
