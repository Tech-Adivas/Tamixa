package com.tamixa.application.stream

import com.tamixa.application.narration.AudioStorageService
import com.tamixa.application.narration.TTSService
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.application.narration.StoryProcessingOrchestrator
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryFamilyVoiceRepositoryPort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.domain.StoryStatus
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.domain.narration.StoryNarrationAudio
import com.tamixa.domain.narration.ToneMode
import com.tamixa.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.tamixa.infrastructure.observability.NarrationMetrics
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.cdn.S3SignedUrlGenerator
import com.tamixa.infrastructure.observability.StreamAccessLogger
import com.tamixa.infrastructure.observability.StreamMetrics
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
    private val storyLibraryService: StoryLibraryService,
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val storyRepository: StoryRepositoryPort,
    private val familyVoiceRepository: StoryFamilyVoiceRepositoryPort,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val subscriptionGuard: SubscriptionGuard,
    private val voiceCatalog: NarrationVoiceCatalogRepositoryPort,
    private val narrationMetrics: NarrationMetrics,
    private val streamAccessLogger: StreamAccessLogger,
    private val streamMetrics: StreamMetrics,
    private val voiceRepository: VoiceRepositoryPort,
    private val narrationOrchestrator: StoryProcessingOrchestrator,
    @Autowired(required = false) private val ttsService: TTSService?,
    @Autowired(required = false) private val audioStorage: AudioStorageService?,
    @Autowired(required = false) private val s3SignedUrlGenerator: S3SignedUrlGenerator?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val PATH_TEMPLATE = "stories/%d/%s/audio.mp3"
        private const val NARRATION_PATH_TEMPLATE = "stories/%d/%s/%s.mp3"
        private const val CLONED_PREFIX = "cloned:"
    }

    private fun normalizeLanguage(language: String): String = StreamLanguageUtils.normalize(language)

    private fun voiceProfileSlug(voiceProfile: String): String =
        if (voiceProfile == "default") "v1" else voiceProfile.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50).ifBlank { "v1" }

    /**
     * Returns a signed stream URL for a library story.
     * Uses same resolution as admin preview: story_narration_audio (canonical) so mobile and admin
     * always play the same narrated audio. library_stories.audio_file_url is legacy and can be stale.
     */
    fun getLibraryStreamUrl(
        storyId: Long,
        language: String,
        parentId: Long?
    ): String? {
        val start = System.nanoTime()
        return try {
            val effectiveLang = normalizeLanguage(language)
            val path = storyLibraryService.getNarrationStoragePath(storyId, effectiveLang)
            if (path != null && path.startsWith("stories/")) {
                log.debug("Stream URL storyId={} lang={} path={}", storyId, effectiveLang, path)
                val cacheBust = getNarrationCacheBustEpochMs(storyId, effectiveLang)
                if (preferProxy()) resolveDirectUrl(path, cacheBust)
                else buildAndSignUrl(path, parentId) ?: resolveDirectUrl(path, cacheBust)
            } else {
                log.debug("Stream URL storyId={} lang={} no narration path, fallback to getLibraryNarrationStreamUrl", storyId, effectiveLang)
                getLibraryNarrationStreamUrl(storyId, effectiveLang, "default", parentId)
            }
        } finally {
            streamMetrics.recordStreamUrlGenerationLatency(System.nanoTime() - start)
        }
    }

    /**
     * Returns actual audio duration in seconds for a library story from story_narration_audio.
     * Use for progress bar so displayed length matches real playback.
     */
    fun getLibraryStoryDurationSeconds(storyId: Long, language: String, voiceProfile: String = "default"): Int? {
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, normalizeLanguage(language))
            ?: return null
        val audio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, voiceProfile)
        return if (audio != null && audio.status == NarrationAudioStatus.READY && audio.durationSeconds > 0)
            audio.durationSeconds
        else null
    }

    /**
     * Returns signed URL for family recorded voice. Private path; parent-only.
     */
    fun getFamilyVoiceStreamUrl(storyId: Long, language: String, parentId: Long): String? {
        val effectiveLang = normalizeLanguage(language)
        val voice = familyVoiceRepository.findByStoryIdAndParentIdAndLanguage(storyId, parentId, effectiveLang)
            ?: return null
        return buildAndSignUrl(voice.storagePath, parentId)
    }

    /**
     * Returns a signed stream URL for library story narration (multi-voice).
     * Uses story_narration_audio. Enforces SubscriptionGuard for premium voices.
     * Triggers on-demand narration for cloned voices when audio does not exist.
     * Cloned voice is always story-specific: narration is generated from this story's
     * translation content and stored per (translationId, voiceProfile); storage path is stories/{storyId}/{language}/{voice}.mp3.
     *
     * @param storyId the specific story; narration and URL are for this story only
     * @param voiceProfile default, calm, etc.; or cloned:{voiceProfileId} for parent's voice
     */
    fun getLibraryNarrationStreamUrl(
        storyId: Long,
        language: String,
        voiceProfile: String,
        parentId: Long?
    ): String? {
        val start = System.nanoTime()
        return try {
            subscriptionGuard.enforcePremiumVoiceAccess(voiceProfile, parentId, language)
            val effectiveLang = normalizeLanguage(language)
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

    /**
     * Returns a signed S3 stream URL for curated narration that is reachable from the public internet.
     * Use when the URL will be fetched by an external service (e.g. Replicate for avatar video).
     * Never returns proxy/localhost URLs. Triggers on-demand cloned voice generation when needed.
     * Per-story: narration is for [storyId] only; storage path includes storyId.
     */
    fun getLibraryNarrationStreamUrlForExternalFetch(
        storyId: Long,
        language: String,
        voiceProfile: String,
        parentId: Long?
    ): String? {
        val start = System.nanoTime()
        return try {
            subscriptionGuard.enforcePremiumVoiceAccess(voiceProfile, parentId, language)
            val effectiveLang = normalizeLanguage(language)
            val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, effectiveLang)
                ?: return null
            var audio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, voiceProfile)
            if (audio == null && voiceProfile.startsWith(CLONED_PREFIX) && parentId != null) {
                audio = generateClonedVoiceOnDemand(storyId, effectiveLang, voiceProfile, translation.id, parentId)
            }
            if (audio == null || audio.status != NarrationAudioStatus.READY) return null
            buildAndSignNarrationUrl(storyId, effectiveLang, voiceProfile, parentId)
        } finally {
            streamMetrics.recordStreamUrlGenerationLatency(System.nanoTime() - start)
        }
    }

    /**
     * Returns the S3 storage path (key) for library story narration for a given voice, or null.
     * Triggers on-demand generation for cloned voices when needed. Used by admin preview-audio with voice.
     */
    fun getLibraryNarrationStoragePath(
        storyId: Long,
        language: String,
        voiceProfile: String,
        parentId: Long?
    ): String? {
        subscriptionGuard.enforcePremiumVoiceAccess(voiceProfile, parentId, language)
        val effectiveLang = normalizeLanguage(language)
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, effectiveLang)
            ?: return null
        var audio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, voiceProfile)
        if (audio == null && voiceProfile.startsWith(CLONED_PREFIX) && parentId != null) {
            audio = generateClonedVoiceOnDemand(storyId, effectiveLang, voiceProfile, translation.id, parentId)
        }
        return if (audio != null && audio.status == NarrationAudioStatus.READY && !audio.audioUrl.isNullOrBlank())
            audio.audioUrl else null
    }

    /**
     * Delete stored narration for (storyId, language, voiceProfile) so the next preview/stream regenerates it.
     * For cloned voices, parentId must own the voice profile.
     * @return true if an entry was deleted
     */
    @Transactional
    fun deleteLibraryStoryNarrationAudio(
        storyId: Long,
        language: String,
        voiceProfile: String,
        parentId: Long?
    ): Boolean {
        subscriptionGuard.enforcePremiumVoiceAccess(voiceProfile, parentId, language)
        val effectiveLang = normalizeLanguage(language)
        if (voiceProfile.startsWith(CLONED_PREFIX) && parentId == null) {
            log.warn("deleteLibraryStoryNarrationAudio: parentId required for cloned voice profile")
            return false
        }
        if (voiceProfile.startsWith(CLONED_PREFIX) && parentId != null) {
            val profileId = voiceProfile.removePrefix(CLONED_PREFIX).trim().toLongOrNull() ?: return false
            val profile = voiceRepository.findByIdAndParentId(profileId, parentId)
                ?: run {
                    log.warn("Cloned voice delete denied: profileId={} not owned by parentId={}", profileId, parentId)
                    return false
                }
        }
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, effectiveLang) ?: return false
        if (narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, voiceProfile) == null) return false
        narrationAudioRepository.deleteByTranslationIdAndVoiceProfile(translation.id, voiceProfile)
        log.info("Deleted narration audio storyId={} lang={} voiceProfile={} (next preview will regenerate)", storyId, effectiveLang, voiceProfile)
        return true
    }

    /**
     * On-demand generation for cloned voice when parent requests stream and no audio exists.
     * Validates ownership. Narration is always for this story only: [translationId] is this story's
     * translation for the given language, and the generated audio is stored per (translationId, voiceProfile).
     */
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
        if (profile.googleVoiceCloningKey.isNullOrBlank() && profile.elevenlabsVoiceId.isNullOrBlank() && profile.referenceAudioPath.isNullOrBlank()) {
            log.warn("Cloned voice profileId={} has no google_voice_cloning_key, elevenlabs_voice_id, or reference_audio_path", profileId)
            return null
        }
        val translation = translationRepository.findById(translationId) ?: return null
        return try {
            log.info("On-demand cloned voice narration storyId={} lang={} voiceProfile={} parentId={}", storyId, language, voiceProfile, parentId)
            narrationOrchestrator.process(
                translation = translation,
                toneMode = ToneMode.CALM,
                voiceProfiles = listOf(voiceProfile),
                parentId = parentId,
                allowClonedForPreview = true
            )
            narrationAudioRepository.findByTranslationIdAndVoiceProfile(translationId, voiceProfile)
        } catch (e: Exception) {
            log.error("On-demand cloned voice narration failed storyId={} voiceProfile={}: {}", storyId, voiceProfile, e.message, e)
            null
        }
    }

    /**
     * Returns signed stream URL for a generated story with cloned voice.
     * When parent requests cloned voice on a generated story (no library translation), we synthesize
     * on-demand and store. Requires TTSService, AudioStorageService, and S3.
     */
    fun getGeneratedClonedStreamUrl(
        storyId: Long,
        language: String,
        voiceProfile: String,
        parentId: Long
    ): String? {
        if (!voiceProfile.startsWith(CLONED_PREFIX) || ttsService == null || audioStorage == null || s3SignedUrlGenerator == null) return null
        val profileId = voiceProfile.removePrefix(CLONED_PREFIX).trim().toLongOrNull() ?: return null
        val profile = voiceRepository.findByIdAndParentId(profileId, parentId)
            ?: run {
                log.warn("Generated cloned voice: profileId={} not owned by parentId={}", profileId, parentId)
                return null
            }
        if (profile.googleVoiceCloningKey.isNullOrBlank() && profile.elevenlabsVoiceId.isNullOrBlank() &&
            profile.referenceAudioPath.isNullOrBlank() && profile.heygenVoiceId.isNullOrBlank()
        ) return null
        val story = storyRepository.findById(storyId) ?: return null
        if (story.status != StoryStatus.READY || story.content.isBlank()) return null
        subscriptionGuard.enforcePremiumVoiceAccess(voiceProfile, parentId, language)
        val effectiveLang = language.trim().lowercase().take(10).ifEmpty { story.language.take(10).ifEmpty { "en" } }
        val plainText = story.content.trim()
        val ssml = "<speak>$plainText</speak>"
        return try {
            log.info("On-demand cloned voice for generated story storyId={} lang={} voiceProfile={}", storyId, effectiveLang, voiceProfile)
            val bytes = ttsService.synthesize(ssml, effectiveLang, voiceProfile)
            if (bytes == null || bytes.isEmpty()) {
                log.warn("Cloned TTS returned empty for generated storyId={}", storyId)
                return null
            }
            val path = audioStorage.uploadNarrationAudio(storyId, effectiveLang, voiceProfile, bytes)
            buildAndSignNarrationUrl(storyId, effectiveLang, voiceProfile, parentId) ?: resolveDirectUrl(path)
        } catch (e: Exception) {
            log.error("Generated story cloned voice failed storyId={} voiceProfile={}: {}", storyId, voiceProfile, e.message, e)
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
        val effectiveLang = normalizeLanguage(language)
        val libraryStory = storyLibraryRepository.findById(storyId)
        if (libraryStory != null) {
            val legacyHasAudio = !libraryStory.audioFileUrl.isNullOrBlank()
            if (legacyHasAudio) return true
            val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, effectiveLang)
            if (translation != null && narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
                    translation.id, "default", NarrationAudioStatus.READY)) return true
            return false
        }
        val story = storyRepository.findById(storyId) ?: return false
        return story.status == StoryStatus.READY
    }
}
