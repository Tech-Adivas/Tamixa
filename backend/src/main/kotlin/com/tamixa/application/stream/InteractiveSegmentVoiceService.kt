package com.tamixa.application.stream

import com.tamixa.application.narration.AudioStorageService
import com.tamixa.application.narration.EmotionToToneMapper
import com.tamixa.application.narration.SSMLBuilderService
import com.tamixa.application.narration.TTSService
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.domain.LibraryStoryStatus
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Interactive story segments in the parent's own (cloned) voice. Segment audio made in admin is in the default
 * voice; this makes one segment's audio in the parent's voice the first time it is asked for, stores it, and reuses
 * it afterwards. The file name starts with "cloned_" so the /audio proxy only serves it with a signed link
 * (see [AudioLinkSigner]), and the voice's media cleanup removes it when the voice is deleted.
 */
@Service
class InteractiveSegmentVoiceService(
    private val appProperties: AppProperties,
    private val storyLibraryService: StoryLibraryService,
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val voiceRepository: VoiceRepositoryPort,
    private val subscriptionGuard: SubscriptionGuard,
    private val ssmlBuilder: SSMLBuilderService,
    private val audioLinkSigner: AudioLinkSigner,
    @Autowired(required = false) private val ttsService: TTSService?,
    @Autowired(required = false) private val audioStorage: AudioStorageService?,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** One generation per file at a time: playback and prefetch often ask for the same segment together. */
    private val locks = ConcurrentHashMap<String, Any>()

    sealed interface Result {
        data class Ready(val url: String) : Result
        /** Story not published in this language, no such segment, or the voice isn't the caller's. */
        data object NotFound : Result
        /** The voice service couldn't make the audio; the app plays the default-voice segment instead. */
        data object Failed : Result
    }

    /**
     * Signed URL of segment [segmentId] of library story [storyId] in [language], spoken in [voiceProfile]
     * ("cloned:<id>", owned by [parentId]). Throws [UpgradeRequiredException] when the plan has no voice cloning.
     */
    fun segmentUrl(storyId: Long, language: String, segmentId: String, voiceProfile: String, parentId: Long): Result {
        val profileId = voiceProfile.trim().takeIf { it.startsWith(CLONED_PREFIX) }
            ?.removePrefix(CLONED_PREFIX)?.trim()?.toLongOrNull()
            ?: return Result.NotFound
        if (voiceRepository.findByIdAndParentId(profileId, parentId) == null) {
            log.warn("Interactive segment voice denied: profileId={} not owned by parentId={}", profileId, parentId)
            return Result.NotFound
        }
        subscriptionGuard.enforcePremiumVoiceAccess(voiceProfile, parentId, language)

        // The graph exactly as this parent's app plays it (published, playback language, language-checked).
        val story = storyLibraryService.findByIdAndLanguage(storyId, language)
            ?.takeIf { it.status == LibraryStoryStatus.PUBLISHED.name && it.narrationApprovedAt != null }
            ?: return Result.NotFound
        val text = story.interactiveGraph?.path("segments")?.path(segmentId)?.path("text")?.asText("")?.trim()
        if (text.isNullOrBlank()) return Result.NotFound
        val lang = language.trim().lowercase()
        val tts = ttsService
        val storage = audioStorage
        if (tts == null || storage == null) {
            log.warn("Interactive segment voice unavailable: TTS or storage not configured")
            return Result.Failed
        }

        // The text's hash is in the name, so edited segment text gets new audio instead of the old recording.
        val fileSlug = "cloned_${profileId}_seg_${slug(segmentId)}_${hash(text)}"
        val key = "stories/$storyId/$lang/$fileSlug.mp3"
        synchronized(locks.computeIfAbsent(key) { Any() }) {
            try {
                if (storage.listNarrationKeys(key).none { it == key }) {
                    val master = storyLibraryRepository.findById(storyId) ?: return Result.NotFound
                    val ssml = ssmlBuilder.buildSSML(text, lang, master.age.coerceIn(1, 21), EmotionToToneMapper.toToneMode(master.emotionMode))
                    log.info("Interactive segment in own voice: generating storyId={} lang={} segment={} profileId={}", storyId, lang, segmentId, profileId)
                    val bytes = tts.synthesize(ssml, lang, voiceProfile)
                    if (bytes == null || bytes.size < MIN_AUDIO_BYTES) {
                        log.warn("Interactive segment in own voice failed storyId={} segment={} profileId={} bytes={}", storyId, segmentId, profileId, bytes?.size)
                        return Result.Failed
                    }
                    val stored = storage.uploadNarrationAudio(storyId, lang, fileSlug, bytes)
                    if (stored != key) log.warn("Interactive segment stored under unexpected key {} (expected {})", stored, key)
                }
            } finally {
                locks.remove(key)
            }
        }
        return Result.Ready(signedUrl(key))
    }

    private fun signedUrl(key: String): String {
        val base = appProperties.audio.publicBaseUrl.trimEnd('/')
        return "$base/audio/$key?${audioLinkSigner.signatureQuery(key)}"
    }

    private fun slug(value: String): String =
        value.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(MAX_SEGMENT_SLUG).ifBlank { "segment" }

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .take(4).joinToString("") { "%02x".format(it) }

    companion object {
        private const val CLONED_PREFIX = "cloned:"
        // The storage adapter keeps 50 characters of the slug: "cloned_" + id + "_seg_" + segment + "_" + 8-char hash.
        private const val MAX_SEGMENT_SLUG = 20
        /** Smaller than this is a provider stub, not speech (segments are 100–150 words). */
        private const val MIN_AUDIO_BYTES = 20_000
    }
}
