package com.araro.application.avatar

import com.araro.application.port.ParentAvatarRepositoryPort
import com.araro.application.port.StoryAvatarVideoRepositoryPort
import com.araro.application.port.StoryAvatarVideoStoragePort
import com.araro.application.stream.AudioStreamService
import com.araro.domain.AvatarVideoStatus
import com.araro.domain.StoryAvatarVideo
import com.araro.infrastructure.avatar.HeyGenAvatarVideoClient
import com.araro.infrastructure.avatar.ReplicateSadTalkerClient
import com.araro.infrastructure.cdn.S3SignedUrlGenerator
import com.araro.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URL
import java.time.Instant

@Service
class AvatarVideoService(
    private val storyAvatarVideoRepository: StoryAvatarVideoRepositoryPort,
    private val parentAvatarRepository: ParentAvatarRepositoryPort,
    private val audioStreamService: AudioStreamService,
    private val appProperties: AppProperties,
    heyGenClientProvider: ObjectProvider<HeyGenAvatarVideoClient>,
    replicateClientProvider: ObjectProvider<ReplicateSadTalkerClient>,
    private val storyAvatarVideoStorageProvider: ObjectProvider<StoryAvatarVideoStoragePort>,
    @Autowired(required = false) private val s3SignedUrlGenerator: S3SignedUrlGenerator?,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val heyGenClient: HeyGenAvatarVideoClient? = heyGenClientProvider.getIfAvailable()
    private val replicateClient: ReplicateSadTalkerClient? = replicateClientProvider.getIfAvailable()
    private val videoStorage: StoryAvatarVideoStoragePort? = storyAvatarVideoStorageProvider.getIfAvailable()

    /**
     * Get signed URL for avatar video if READY. Triggers async generation if not exists.
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
                // Optionally retry - for now return null
                return null
            }
            null -> {
                // Create PENDING record and trigger async generation
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
                storyAvatarVideoRepository.save(record)
                triggerGenerationAsync(storyId, storySource, parentId, language, voiceProfile)
                return null
            }
        }
        return null
    }

    @Async
    fun triggerGenerationAsync(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String
    ) {
        if (videoStorage == null) {
            log.warn("Avatar video: storage not configured")
            return
        }
        val audioUrl = getAudioUrlForReplicate(storyId, storySource, language, voiceProfile, parentId) ?: run {
            markFailed(storyId, storySource, parentId, language, voiceProfile, "No audio URL")
            return
        }
        when {
            heyGenClient != null -> runHeyGenFlow(storyId, storySource, parentId, language, voiceProfile, audioUrl)
            replicateClient != null -> runReplicateFlow(storyId, storySource, parentId, language, voiceProfile, audioUrl)
            else -> {
                log.warn("Avatar video: no provider configured (heygen or sadtalker)")
                markFailed(storyId, storySource, parentId, language, voiceProfile, "No avatar video provider")
            }
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
        val talkingPhotoId = heyGenClient!!.uploadTalkingPhoto(imageBytes, contentType) ?: run {
            markFailed(storyId, storySource, parentId, language, voiceProfile, "HeyGen talking photo upload failed")
            return
        }
        val videoId = heyGenClient.createVideo(talkingPhotoId, audioUrl) ?: run {
            markFailed(storyId, storySource, parentId, language, voiceProfile, "HeyGen create video failed")
            return
        }
        updatePredictionId(storyId, storySource, parentId, language, voiceProfile, videoId)
        pollAndCompleteHeyGen(storyId, storySource, parentId, language, voiceProfile, videoId)
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
            markFailed(storyId, storySource, parentId, language, voiceProfile, "No avatar image")
            return
        }
        val predictionId = replicateClient!!.createPrediction(avatarUrl, audioUrl) ?: run {
            markFailed(storyId, storySource, parentId, language, voiceProfile, "Replicate create failed")
            return
        }
        updatePredictionId(storyId, storySource, parentId, language, voiceProfile, predictionId)
        pollAndCompleteReplicate(storyId, storySource, parentId, language, voiceProfile, predictionId)
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

    private fun pollAndCompleteReplicate(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String,
        predictionId: String
    ) {
        val props = appProperties.avatarVideo
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
        markFailed(storyId, storySource, parentId, language, voiceProfile, "Timeout waiting for Replicate")
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
            val bytes = URL(videoUrl).openStream().readBytes()
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
            "curated" -> audioStreamService.getCuratedStreamUrl(storyId, language, parentId)
                ?: audioStreamService.getCuratedNarrationStreamUrl(storyId, language, voiceProfile, parentId)
            "generated" -> audioStreamService.getGeneratedStreamUrl(storyId, language, parentId)
            else -> null
        }
        return url != null
    }

    private fun getAvatarImageUrlForReplicate(parentId: Long): String? {
        val avatar = parentAvatarRepository.findByParentId(parentId) ?: return null
        return buildSignedUrl(avatar.storagePath) ?: return null
    }

    private fun getAudioUrlForReplicate(
        storyId: Long,
        storySource: String,
        language: String,
        voiceProfile: String,
        parentId: Long?
    ): String? {
        return when (storySource) {
            "curated" -> {
                if (voiceProfile.equals("family", ignoreCase = true) && parentId != null)
                    audioStreamService.getFamilyVoiceStreamUrl(storyId, language, parentId)
                else
                    audioStreamService.getCuratedNarrationStreamUrl(storyId, language, voiceProfile, parentId)
                        ?: audioStreamService.getCuratedStreamUrl(storyId, language, parentId)
            }
            "generated" -> audioStreamService.getGeneratedStreamUrl(storyId, language, parentId)
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
