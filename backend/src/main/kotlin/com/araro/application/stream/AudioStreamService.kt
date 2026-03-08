package com.araro.application.stream

import com.araro.application.curated.CuratedStoryService
import com.araro.application.narration.StoryProcessingOrchestrator
import com.araro.application.port.CuratedStoryRepositoryPort
import com.araro.application.port.StoryAudioRepositoryPort
import com.araro.application.port.StoryFamilyVoiceRepositoryPort
import com.araro.application.port.StoryRepositoryPort
import com.araro.application.port.VoiceRepositoryPort
import com.araro.application.port.narration.StoryNarrationAudioRepositoryPort
import com.araro.application.port.StoryTranslationRepositoryPort
import com.araro.domain.StoryStatus
import com.araro.domain.narration.NarrationAudioStatus
import com.araro.domain.narration.StoryNarrationAudio
import com.araro.domain.narration.ToneMode
import com.araro.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.araro.infrastructure.observability.NarrationMetrics
import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.cdn.S3SignedUrlGenerator
import com.araro.infrastructure.observability.StreamAccessLogger
import com.araro.infrastructure.observability.StreamMetrics
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Generates short-lived signed URLs for audio streaming via S3.
 * Path conventions:
 * - story_audio (translation): stories/{storyId}/{language}/audio.mp3
 * - narration (multi-voice): stories/{storyId}/{language}/{voiceSlug}.mp3
 *
 * Premium voice gating enforced via SubscriptionGuard before returning URL.
 */
@Service
class AudioStreamService(
    private val appProperties: AppProperties,
    private val curatedStoryService: CuratedStoryService,
    private val curatedStoryRepository: CuratedStoryRepositoryPort,
    private val storyRepository: StoryRepositoryPort,
    private val familyVoiceRepository: StoryFamilyVoiceRepositoryPort,
    private val storyAudioRepository: StoryAudioRepositoryPort,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val subscriptionGuard: SubscriptionGuard,
    private val voiceCatalog: NarrationVoiceCatalogRepositoryPort,
    private val narrationMetrics: NarrationMetrics,
    private val streamAccessLogger: StreamAccessLogger,
    private val streamMetrics: StreamMetrics,
    private val voiceRepository: VoiceRepositoryPort,
    private val narrationOrchestrator: StoryProcessingOrchestrator,
    @Autowired(required = false) private val s3SignedUrlGenerator: S3SignedUrlGenerator?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val PATH_TEMPLATE = "stories/%d/%s/audio.mp3"
        private const val NARRATION_PATH_TEMPLATE = "stories/%d/%s/%s.mp3"
        private const val CLONED_PREFIX = "cloned:"
    }

    private fun voiceProfileSlug(voiceProfile: String): String =
        if (voiceProfile == "default") "v1" else voiceProfile.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50).ifBlank { "v1" }

    /**
     * Returns a signed stream URL for a curated story.
     * Uses same resolution as admin preview: story_narration_audio (canonical) so mobile and admin
     * always play the same narrated audio. curated_stories.audio_file_url is legacy and can be stale.
     */
    fun getCuratedStreamUrl(
        storyId: Long,
        language: String,
        parentId: Long?
    ): String? {
        val start = System.nanoTime()
        return try {
            val effectiveLang = language.trim().lowercase().take(10).ifEmpty { "ta" }
            val path = curatedStoryService.getNarrationStoragePath(storyId, effectiveLang)
                ?: run {
                    if (effectiveLang != "ta") {
                        storyAudioRepository.findByMasterStoryIdAndLanguage(storyId, effectiveLang)?.audioFileUrl?.takeIf { it.startsWith("stories/") }
                    } else null
                }
            if (path != null && path.startsWith("stories/")) {
                log.debug("Stream URL storyId={} lang={} path={}", storyId, effectiveLang, path)
                val cacheBust = getNarrationCacheBustEpochMs(storyId, effectiveLang)
                if (preferProxy()) resolveDirectUrl(path, cacheBust)
                else buildAndSignUrl(path, parentId) ?: resolveDirectUrl(path, cacheBust)
            } else {
                log.debug("Stream URL storyId={} lang={} no narration path, fallback to getCuratedNarrationStreamUrl", storyId, effectiveLang)
                getCuratedNarrationStreamUrl(storyId, effectiveLang, "default", parentId)
            }
        } finally {
            streamMetrics.recordStreamUrlGenerationLatency(System.nanoTime() - start)
        }
    }

    /**
     * Returns signed URL for family recorded voice. Private path; parent-only.
     */
    fun getFamilyVoiceStreamUrl(storyId: Long, language: String, parentId: Long): String? {
        val effectiveLang = language.trim().lowercase().take(10).ifEmpty { "ta" }
        val voice = familyVoiceRepository.findByStoryIdAndParentIdAndLanguage(storyId, parentId, effectiveLang)
            ?: return null
        return buildAndSignUrl(voice.storagePath, parentId)
    }

    /**
     * Returns a signed stream URL for curated story narration (multi-voice).
     * Uses story_narration_audio. Enforces SubscriptionGuard for premium voices.
     * Triggers on-demand narration for cloned voices when audio does not exist.
     * @param voiceProfile default, calm, etc.; or cloned:{voiceProfileId} for parent's voice
     */
    fun getCuratedNarrationStreamUrl(
        storyId: Long,
        language: String,
        voiceProfile: String,
        parentId: Long?
    ): String? {
        val start = System.nanoTime()
        return try {
            subscriptionGuard.enforcePremiumVoiceAccess(voiceProfile, parentId, language)
            val effectiveLang = language.trim().lowercase().take(10).ifEmpty { "ta" }
            val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, effectiveLang)
                ?: return null
            var audio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, voiceProfile)
            if (audio == null && voiceProfile.startsWith(CLONED_PREFIX) && parentId != null) {
                audio = generateClonedVoiceOnDemand(storyId, effectiveLang, voiceProfile, translation.id, parentId)
            }
            if (audio == null || audio.status != NarrationAudioStatus.READY) return null
            voiceCatalog.findByToneMode(voiceProfile)?.takeIf { it.isPremium }?.let { narrationMetrics.recordPremiumVoiceUsage() }
            val cacheBust = audio.createdAt.toEpochMilli()
            if (preferProxy()) resolveDirectUrl(audio.audioUrl, cacheBust)
            else buildAndSignNarrationUrl(storyId, effectiveLang, voiceProfile, parentId) ?: resolveDirectUrl(audio.audioUrl, cacheBust)
        } finally {
            streamMetrics.recordStreamUrlGenerationLatency(System.nanoTime() - start)
        }
    }

    /** On-demand generation for cloned voice when parent requests stream and no audio exists. Validates ownership. */
    private fun generateClonedVoiceOnDemand(
        storyId: Long,
        language: String,
        voiceProfile: String,
        translationId: Long,
        parentId: Long
    ): StoryNarrationAudio? {
        val profileId = voiceProfile.removePrefix(CLONED_PREFIX).trim().toLongOrNull() ?: return null
        val profile = voiceRepository.findByIdAndParentId(profileId, parentId)
            ?: run {
                log.warn("Cloned voice access denied: profileId={} not owned by parentId={}", profileId, parentId)
                return null
            }
        if (profile.elevenlabsVoiceId.isNullOrBlank()) {
            log.warn("Cloned voice profileId={} has no elevenlabs_voice_id", profileId)
            return null
        }
        val translation = translationRepository.findById(translationId) ?: return null
        return try {
            log.info("On-demand cloned voice narration storyId={} lang={} voiceProfile={} parentId={}", storyId, language, voiceProfile, parentId)
            narrationOrchestrator.process(
                translation = translation,
                toneMode = ToneMode.CALM,
                voiceProfiles = listOf(voiceProfile),
                parentId = parentId
            )
            narrationAudioRepository.findByTranslationIdAndVoiceProfile(translationId, voiceProfile)
        } catch (e: Exception) {
            log.error("On-demand cloned voice narration failed storyId={} voiceProfile={}: {}", storyId, voiceProfile, e.message, e)
            null
        }
    }

    /**
     * Returns a signed stream URL for an AI-generated story.
     * When CDN disabled, falls back to direct audioFileUrl resolved to full URL.
     */
    fun getGeneratedStreamUrl(
        storyId: Long,
        language: String,
        parentId: Long?
    ): String? {
        val start = System.nanoTime()
        return try {
            val story = storyRepository.findById(storyId) ?: return null
            if (story.status != StoryStatus.READY) {
                log.info("Stream URL null: generated story {} not READY (status={}). Mobile may fall back to device TTS (dry reading).", storyId, story.status)
                return null
            }
            if (story.audioFileUrl.isNullOrBlank()) {
                log.info("Stream URL null: generated story {} has no audio yet. Mobile may fall back to device TTS (dry reading).", storyId)
                return null
            }
            val effectiveLang = story.language.trim().lowercase().take(10).ifEmpty { "en" }
            if (preferProxy()) resolveDirectUrl(story.audioFileUrl)
            else buildAndSignUrl(storyId, effectiveLang, parentId) ?: resolveDirectUrl(story.audioFileUrl)
        } finally {
            streamMetrics.recordStreamUrlGenerationLatency(System.nanoTime() - start)
        }
    }

    private fun preferProxy(): Boolean {
        val b = appProperties.audio.publicBaseUrl
        return b.isNotBlank() && (b.contains("10.0.2.2") || b.contains("localhost") || b.contains("127.0.0.1"))
    }

    /** Returns narration audio createdAt as epoch ms for cache busting when using direct URLs. */
    private fun getNarrationCacheBustEpochMs(storyId: Long, language: String): Long? {
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, language) ?: return null
        val audio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, "default") ?: return null
        if (audio.status != NarrationAudioStatus.READY) return null
        return audio.createdAt.toEpochMilli()
    }

    private fun resolveDirectUrl(audioFileUrl: String?, cacheBustEpochMs: Long? = null): String? {
        if (audioFileUrl.isNullOrBlank()) return null
        val base = appProperties.audio.publicBaseUrl.trimEnd('/')
        val suffix = if (cacheBustEpochMs != null) "?v=$cacheBustEpochMs" else ""
        val raw = when {
            audioFileUrl.startsWith("https://") -> audioFileUrl
            audioFileUrl.startsWith("http://") -> {
                val url = audioFileUrl
                if (url.contains("localhost") || url.contains("127.0.0.1")) {
                    val path = url.substringAfter("/audio/").takeIf { it != url } ?: url.substringAfter(base)
                    if (path.startsWith("stories/")) "$base/audio/$path" else url
                } else url
            }
            audioFileUrl.startsWith("/") -> base + audioFileUrl
            else -> "$base/audio/$audioFileUrl"
        }
        return raw + suffix
    }

    private fun buildAndSignUrl(storyId: Long, language: String, parentId: Long?): String? {
        val objectPath = PATH_TEMPLATE.format(storyId, language)
        val expiryMinutes = appProperties.cdn.signedUrlExpiryMinutes
        val signedUrl = when {
            appProperties.storage.type == "s3" && s3SignedUrlGenerator != null ->
                s3SignedUrlGenerator.signUrl(objectPath, expiryMinutes)?.toString()
            else -> null
        } ?: return null
        streamAccessLogger.logStreamAccess(storyId = storyId, language = language, parentId = parentId, path = objectPath)
        return signedUrl
    }

    private fun buildAndSignUrl(objectPath: String, parentId: Long?): String? {
        val expiryMinutes = appProperties.cdn.signedUrlExpiryMinutes
        val signedUrl = when {
            appProperties.storage.type == "s3" && s3SignedUrlGenerator != null ->
                s3SignedUrlGenerator.signUrl(objectPath, expiryMinutes)?.toString()
            else -> null
        } ?: return null
        streamAccessLogger.logStreamAccess(storyId = 0, language = "", parentId = parentId, path = objectPath)
        return signedUrl
    }

    private fun buildAndSignNarrationUrl(storyId: Long, language: String, voiceProfile: String, parentId: Long?): String? {
        val slug = voiceProfileSlug(voiceProfile)
        val objectPath = NARRATION_PATH_TEMPLATE.format(storyId, language, slug)
        val expiryMinutes = 10L.coerceAtMost(appProperties.cdn.signedUrlExpiryMinutes)
        val signedUrl = when {
            appProperties.storage.type == "s3" && s3SignedUrlGenerator != null ->
                s3SignedUrlGenerator.signUrl(objectPath, expiryMinutes)?.toString()
            else -> null
        } ?: return null
        streamAccessLogger.logStreamAccess(storyId = storyId, language = language, parentId = parentId, path = objectPath)
        return signedUrl.toString()
    }

    /**
     * Whether the given path should be cached at CDN.
     * PENDING stories are not cached.
     */
    fun shouldCache(storyId: Long, language: String): Boolean {
        val curated = curatedStoryRepository.findById(storyId)
        if (curated != null) {
            val legacyHasAudio = if (language == "ta") !curated.audioFileUrl.isNullOrBlank()
            else storyAudioRepository.findByMasterStoryIdAndLanguage(storyId, language) != null
            if (legacyHasAudio) return true
            val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, language)
            if (translation != null && narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
                    translation.id, "default", NarrationAudioStatus.READY)) return true
            return false
        }
        val story = storyRepository.findById(storyId) ?: return false
        return story.status == StoryStatus.READY
    }
}
