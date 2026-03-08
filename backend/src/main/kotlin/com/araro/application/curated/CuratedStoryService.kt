package com.araro.application.curated

import com.araro.api.admin.CuratedStoryMapper.toResponse
import com.araro.api.admin.dto.CuratedStoryListingResponse
import com.araro.api.admin.dto.CuratedStoryResponse
import com.araro.application.port.CuratedStoryEventPublisherPort
import com.araro.application.port.CuratedStoryRepositoryPort
import com.araro.application.port.StoryAudioRepositoryPort
import com.araro.application.port.StoryTranslationRepositoryPort
import com.araro.application.port.narration.StoryNarrationAudioRepositoryPort
import com.araro.application.stream.CoverImageUrlResolver
import com.araro.infrastructure.persistence.FavoriteStoryJpaRepository
import com.araro.infrastructure.persistence.StoryAnalyticsJpaRepository
import com.araro.infrastructure.persistence.StoryFeedbackJpaRepository
import com.araro.infrastructure.persistence.StoryPlaybackPositionJpaRepository
import com.araro.domain.TranslationPipelineStatus
import com.araro.domain.narration.NarrationAudioStatus
import com.araro.domain.CuratedStory
import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.observability.PipelineProgressTracker
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

@Service
class CuratedStoryService(
    private val repository: CuratedStoryRepositoryPort,
    private val eventPublisher: CuratedStoryEventPublisherPort,
    private val storyTranslationRepository: StoryTranslationRepositoryPort,
    private val storyAudioRepository: StoryAudioRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    @Autowired(required = false) private val s3Client: S3Client?,
    private val favoriteStoryJpaRepository: FavoriteStoryJpaRepository,
    private val storyAnalyticsJpaRepository: StoryAnalyticsJpaRepository,
    private val storyPlaybackPositionJpaRepository: StoryPlaybackPositionJpaRepository,
    private val storyFeedbackJpaRepository: StoryFeedbackJpaRepository,
    private val appProperties: AppProperties,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    private val progressTracker: PipelineProgressTracker
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val languageAliases = mapOf(
            "tamil" to "ta", "english" to "en", "hindi" to "hi",
            "telugu" to "te", "kannada" to "kn", "malayalam" to "ml"
        )
    }

    private fun effectiveLanguage(language: String): String {
        val normalized = language.trim().lowercase()
        return languageAliases[normalized] ?: normalized
    }

    /**
     * Resolves audio URL for display: story_audio first, then story_narration_audio.
     * Used when curated_stories.audio_file_url is null but pipeline has stored audio in story_narration_audio.
     */
    fun resolveAudioFileUrlForDisplay(masterStoryId: Long, language: String): String? =
        resolveAudioFileUrl(masterStoryId, language)

    /**
     * Returns the storage path (S3 key) for narration audio, or null if not found.
     * Used by admin stream endpoint to fetch and serve audio bytes.
     */
    fun getNarrationStoragePath(masterStoryId: Long, language: String): String? =
        resolveAudioFileUrl(masterStoryId, language)?.takeIf { it.startsWith("stories/") }

    /**
     * Resolves canonical audio path. Prefers story_narration_audio (conversational) over story_audio
     * (legacy flat). Ensures mobile and admin always play the same narrated audio.
     */
    private fun resolveAudioFileUrl(masterStoryId: Long, language: String): String? {
        val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language) ?: return null
        val narration = narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, "default")
        if (narration != null && narration.status == NarrationAudioStatus.READY && !narration.audioUrl.isNullOrBlank()) {
            return narration.audioUrl
        }
        storyAudioRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.audioFileUrl?.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }

    /**
     * Returns playable audio URL for mobile: full URL when publicBaseUrl is set, else raw path.
     * Narration paths (stories/...) become {base}/audio/{path}?v={createdAt} for cache busting.
     */
    private fun resolvePlayableAudioUrl(masterStoryId: Long, language: String): String? {
        val path = resolveAudioFileUrl(masterStoryId, language) ?: return null
        return pathToPlayableUrl(path, masterStoryId, language)
    }

    private fun pathToPlayableUrl(path: String, masterStoryId: Long, language: String): String {
        val base = appProperties.audio.publicBaseUrl.trimEnd('/')
        return when {
            path.startsWith("stories/") && base.isNotBlank() -> {
                val cacheBust = runCatching {
                    val t = storyTranslationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language) ?: return@runCatching null
                    narrationAudioRepository.findByTranslationIdAndVoiceProfile(t.id, "default")?.createdAt?.toEpochMilli()
                }.getOrNull()
                val suffix = cacheBust?.let { "?v=$it" } ?: ""
                "$base/audio/$path$suffix"
            }
            else -> path
        }
    }

    /**
     * Create curated story from text. Audio is generated asynchronously via TTS pipeline.
     * Admin uploads text only; system generates audio automatically.
     * Validates: min word count, Tamil script (for ta), duplicate title, category required.
     */
    @Transactional
    fun create(
        title: String?,
        content: String,
        theme: String,
        language: String = "ta",
        age: Int,
        childName: String = "Child",
        moral: String?,
        audioFileUrl: String? = null,  // Ignored; system generates audio from text
        status: String = "DRAFT",
        coverImageUrl: String? = null,
        emotionMode: String? = null
    ): CuratedStory {
        // Validation: category/theme required (enforced by @NotBlank on theme)
        // Min word count
        CuratedStoryValidation.validateMinWordCount(content).getOrElse { throw it }
        // Tamil script check for Tamil language
        if (language.trim().lowercase() == "ta") {
            CuratedStoryValidation.validateTamilScript(content).getOrElse { throw it }
        }
        // Duplicate title (when title provided)
        title?.takeIf { it.isNotBlank() }?.let { t ->
            if (repository.existsByTitle(t)) {
                throw IllegalArgumentException("A story with this title already exists")
            }
        }

        val wordCount = content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val readingTimeMinutes = (wordCount / 150.0).coerceAtMost(5.0)
        val now = java.time.Instant.now()
        val effectiveStatus = status?.take(20)?.uppercase()?.let { if (it in listOf("DRAFT", "PUBLISHED")) it else "DRAFT" } ?: "DRAFT"
        val effectiveEmotion = emotionMode?.trim()?.uppercase()?.take(20)
            ?.let { if (it in listOf("CALM", "SOOTHING", "ADVENTUROUS")) it else "CALM" } ?: "CALM"
        val story = CuratedStory(
            id = 0,
            title = title,
            content = content,
            theme = theme,
            language = language.trim().lowercase().take(10).ifEmpty { "ta" },
            age = age.coerceIn(1, 12),
            childName = childName.ifBlank { "Child" }.take(255),
            wordCount = wordCount,
            readingTimeMinutes = readingTimeMinutes,
            moral = moral,
            audioFileUrl = null,  // System generates via TTS pipeline
            status = effectiveStatus,
            coverImageUrl = coverImageUrl?.take(512),
            createdAt = now,
            emotionMode = effectiveEmotion
        )
        val saved = repository.save(story)
        eventPublisher.publishCuratedStoryCreated(saved.id, saved.content)
        return saved
    }

    fun findById(id: Long): CuratedStory? = repository.findById(id)

    fun findByIdAndLanguage(id: Long, language: String): CuratedStoryResponse? {
        val effectiveLang = effectiveLanguage(language)
        return when {
            effectiveLang == "ta" -> {
                val master = repository.findById(id) ?: return null
                // Prefer narrated script (translation.content) when available; else master.content
                val taTranslation = storyTranslationRepository.findByMasterStoryIdAndLanguage(id, effectiveLang)
                val displayContent = taTranslation?.content?.takeIf { it.isNotBlank() } ?: master.content
                // Prefer narration (story_narration_audio) over legacy (curated_stories.audio_file_url).
                val canonicalAudio = resolvePlayableAudioUrl(id, effectiveLang)
                    ?: (master.audioFileUrl?.takeIf { it.isNotBlank() }
                        ?.let { if (it.startsWith("stories/")) pathToPlayableUrl(it, id, effectiveLang) else it })
                master.toResponse(
                    coverImageUrlResolver.resolveCoverPath(master.coverImageUrl),
                    coverImageUrlResolver.resolveCoverVideoPath(master.coverVideoUrl)
                ).copy(
                    content = displayContent,
                    wordCount = taTranslation?.wordCount ?: master.wordCount,
                    readingTimeMinutes = taTranslation?.readingTimeMinutes ?: master.readingTimeMinutes,
                    audioFileUrl = canonicalAudio
                )
            }
            else -> {
                val master = repository.findById(id) ?: return null
                val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(id, effectiveLang)
                    ?: return null
                CuratedStoryResponse(
                    id = master.id,
                    title = translation.title,
                    content = translation.content,
                    theme = master.theme,
                    language = effectiveLang,
                    age = master.age,
                    childName = master.childName,
                    wordCount = translation.wordCount,
                    readingTimeMinutes = translation.readingTimeMinutes,
                    moral = translation.moral,
                    audioFileUrl = resolvePlayableAudioUrl(id, effectiveLang),
                    status = master.status,
                    coverImageUrl = coverImageUrlResolver.resolveCoverPath(master.coverImageUrl),
                    coverVideoUrl = coverImageUrlResolver.resolveCoverVideoPath(master.coverVideoUrl),
                    createdAt = master.createdAt,
                    emotionMode = master.emotionMode
                )
            }
        }
    }

    fun findAll(page: Int, size: Int): Page<CuratedStory> =
        repository.findAll(PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100)))

    /**
     * Listing with projection (no content). Use for list views.
     */
    fun findListingByLanguage(language: String, page: Int, size: Int): Page<CuratedStoryListingResponse> {
        val effectiveLang = effectiveLanguage(language)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        return when {
            effectiveLang == "ta" -> repository.findListingByLanguage("ta", pageable).map { listing ->
                CuratedStoryListingResponse(
                    id = listing.id,
                    title = listing.title,
                    theme = listing.theme,
                    language = effectiveLang,
                    age = listing.age,
                    childName = listing.childName,
                    wordCount = listing.wordCount,
                    readingTimeMinutes = listing.readingTimeMinutes,
                    audioFileUrl = resolvePlayableAudioUrl(listing.id, effectiveLang),
                    status = listing.status,
                    coverImageUrl = coverImageUrlResolver.resolveCoverPath(listing.coverImageUrl),
                    coverVideoUrl = coverImageUrlResolver.resolveCoverVideoPath(listing.coverVideoUrl),
                    createdAt = listing.createdAt
                )
            }
            else -> {
                val translations = storyTranslationRepository.findListingByLanguage(effectiveLang, pageable)
                val masterIds = translations.content.map { it.masterStoryId }.distinct()
                val masters = repository.findListingByIdIn(masterIds).associateBy { it.id }
                val content = translations.content.map { t ->
                    val master = masters[t.masterStoryId]!!
                    CuratedStoryListingResponse(
                        id = master.id,
                        title = t.title,
                        theme = master.theme,
                        language = effectiveLang,
                        age = master.age,
                        childName = master.childName,
                        wordCount = t.wordCount,
                        readingTimeMinutes = t.readingTimeMinutes,
                        audioFileUrl = resolvePlayableAudioUrl(t.masterStoryId, effectiveLang),
                        status = master.status,
                        coverImageUrl = coverImageUrlResolver.resolveCoverPath(master.coverImageUrl),
                        coverVideoUrl = coverImageUrlResolver.resolveCoverVideoPath(master.coverVideoUrl),
                        createdAt = master.createdAt
                    )
                }
                PageImpl(content, translations.pageable, translations.totalElements)
            }
        }
    }

    /**
     * Tamil: from curated_stories. Other languages: from story_translations joined with master + audio.
     * Backward compatible: Tamil serving unchanged.
     */
    fun findByLanguage(language: String, page: Int, size: Int): Page<CuratedStoryResponse> {
        val effectiveLang = effectiveLanguage(language)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        return when {
            effectiveLang == "ta" -> repository.findByLanguage("ta", pageable).map {
                val canonicalAudio = resolvePlayableAudioUrl(it.id, "ta")
                    ?: (it.audioFileUrl?.takeIf { u -> u.isNotBlank() }
                        ?.let { p -> if (p.startsWith("stories/")) pathToPlayableUrl(p, it.id, "ta") else p })
                it.toResponse(
                    coverImageUrlResolver.resolveCoverPath(it.coverImageUrl),
                    coverImageUrlResolver.resolveCoverVideoPath(it.coverVideoUrl)
                ).copy(
                    audioFileUrl = canonicalAudio
                )
            }
            else -> {
                val translations = storyTranslationRepository.findByLanguage(effectiveLang, pageable)
                translations.map { t ->
                    val master = repository.findById(t.masterStoryId)!!
                    CuratedStoryResponse(
                        id = master.id,
                        title = t.title,
                        content = t.content,
                        theme = master.theme,
                        language = effectiveLang,
                        age = master.age,
                        childName = master.childName,
                        wordCount = t.wordCount,
                        readingTimeMinutes = t.readingTimeMinutes,
                        moral = t.moral,
                        audioFileUrl = resolvePlayableAudioUrl(t.masterStoryId, effectiveLang),
                        status = master.status,
                        coverImageUrl = coverImageUrlResolver.resolveCoverPath(master.coverImageUrl),
                        coverVideoUrl = coverImageUrlResolver.resolveCoverVideoPath(master.coverVideoUrl),
                        createdAt = master.createdAt
                    )
                }
            }
        }
    }

    fun findAllByStatus(status: String?, page: Int, size: Int): Page<CuratedStory> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        return if (status != null && status.isNotBlank()) {
            val s = status.trim().uppercase()
            if (s == "PUBLISHED") {
                repository.findByStatusInWithProcessingFirst(listOf("PUBLISHED", "PROCESSING", "READY"), pageable)
            } else {
                repository.findByStatus(s, pageable)
            }
        } else {
            repository.findAllWithProcessingFirst(pageable)
        }
    }

    fun bulkPublish(ids: List<Long>): Int =
        repository.updateStatusBulk(ids, "PUBLISHED")

    fun bulkUpdateCategory(ids: List<Long>, theme: String): Int =
        repository.updateThemeBulk(ids, theme.trim().take(100))

    /**
     * Remove legacy audio (story_audio + curated_stories.audio_file_url) so only story_narration_audio is used.
     * Deletes S3 objects for legacy paths (e.g. stories/{id}/{lang}/audio.mp3) when S3 is configured.
     * Use after switching to Chirp3-HD / improved pipeline. Stories will use narration pipeline audio only.
     */
    @Transactional
    fun clearLegacyAudio(): Triple<Int, Int, Int> {
        val legacyRows = storyAudioRepository.findAllLegacy()
        val s3KeysToDelete = legacyRows
            .mapNotNull { it.audioFileUrl?.takeIf { url -> url.startsWith("stories/") } }
            .filter { !it.endsWith("v1.mp3") }  // Never delete narration audio
            .toSet()
        var s3Deleted = 0
        if (s3Client != null && s3KeysToDelete.isNotEmpty() && appProperties.storage.type == "s3") {
            val bucket = appProperties.storage.s3Bucket.ifBlank { "araro-audio" }
            for (key in s3KeysToDelete) {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
                    s3Deleted++
                    log.debug("S3 legacy audio deleted key={}", key)
                } catch (e: Exception) {
                    log.warn("S3 delete failed for key={}: {}", key, e.message)
                }
            }
            log.info("S3 legacy audio objects deleted: {} of {}", s3Deleted, s3KeysToDelete.size)
        }
        val deletedAudio = storyAudioRepository.deleteAllLegacy()
        val clearedUrls = repository.clearAllAudioUrls()
        log.info("Legacy audio cleared: story_audio rows deleted={}, curated_stories audio_file_url cleared={}, S3 objects deleted={}", deletedAudio, clearedUrls, s3Deleted)
        return Triple(deletedAudio, clearedUrls, s3Deleted)
    }

    /**
     * Delete a curated story and all related data (favorites, family voices, analytics, playback, feedback).
     * Cascades to story_translations, story_audio, story_narration_audio, story_processing_status via DB.
     */
    @Transactional
    fun deleteById(id: Long): Boolean {
        if (repository.findById(id) == null) return false
        cleanupRelatedData(id)
        repository.deleteById(id)
        log.info("Curated story deleted id={}", id)
        return true
    }

    /**
     * Bulk delete curated stories. Returns count of deleted stories.
     */
    @Transactional
    fun deleteByIds(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        val existing = ids.filter { repository.findById(it) != null }
        existing.forEach { cleanupRelatedData(it) }
        existing.forEach { repository.deleteById(it) }
        log.info("Curated stories bulk deleted ids={} count={}", existing, existing.size)
        return existing.size
    }

    private fun cleanupRelatedData(storyId: Long) {
        // Do NOT delete family voice recordings: they are the parent's voice profile used for any story.
        favoriteStoryJpaRepository.deleteByStoryIdAndStorySource(storyId, "curated")
        storyAnalyticsJpaRepository.deleteByStoryIdAndStorySource(storyId, "curated")
        storyPlaybackPositionJpaRepository.deleteByStoryIdAndStorySource(storyId, "curated")
        storyFeedbackJpaRepository.deleteByStoryIdAndStorySource(storyId, "curated")
    }

    @Transactional
    fun update(
        id: Long,
        title: String?,
        content: String,
        theme: String,
        language: String,
        age: Int,
        childName: String,
        moral: String?,
        status: String,
        coverImageUrl: String?,
        coverVideoUrl: String? = null,
        emotionMode: String? = null,
        updateNarratedOnly: Boolean = false
    ): CuratedStory? {
        log.info("Curated story update id={} status={} updateNarratedOnly={}", id, status, updateNarratedOnly)
        val existing = repository.findById(id) ?: return null
        CuratedStoryValidation.validateMinWordCount(content).getOrElse { throw it }
        if (language.trim().lowercase() == "ta") {
            CuratedStoryValidation.validateTamilScript(content).getOrElse { throw it }
        }
        title?.takeIf { it.isNotBlank() }?.let { t ->
            if (repository.existsByTitle(t) && t != existing.title) {
                throw IllegalArgumentException("A story with this title already exists")
            }
        }
        val wordCount = content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val readingTimeMinutes = (wordCount / 150.0).coerceAtMost(5.0)
        val effectiveLang = language.trim().lowercase().take(10).ifEmpty { "ta" }
        val effectiveEmotion = emotionMode?.trim()?.uppercase()?.take(20)
            ?.let { if (it in listOf("CALM", "SOOTHING", "ADVENTUROUS")) it else existing.emotionMode ?: "CALM" }
            ?: (existing.emotionMode ?: "CALM")
        // Preserve existing cover URLs when request sends null/blank so generated AI cover is not lost on publish.
        val effectiveCoverImage = coverImageUrl?.takeIf { it.isNotBlank() }?.take(512) ?: existing.coverImageUrl
        val effectiveCoverVideo = coverVideoUrl?.takeIf { it.isNotBlank() }?.take(512) ?: existing.coverVideoUrl
        val updated = CuratedStory(
            id = existing.id,
            title = title?.takeIf { it.isNotBlank() },
            content = content,
            theme = theme.trim().take(100),
            language = effectiveLang,
            age = age.coerceIn(1, 12),
            childName = childName.ifBlank { "Child" }.take(255),
            wordCount = wordCount,
            readingTimeMinutes = readingTimeMinutes,
            moral = moral?.takeIf { it.isNotBlank() },
            audioFileUrl = existing.audioFileUrl,
            status = status.take(20).uppercase().let { if (it in listOf("DRAFT", "PUBLISHED")) it else existing.status },
            coverImageUrl = effectiveCoverImage,
            coverVideoUrl = effectiveCoverVideo,
            createdAt = existing.createdAt,
            emotionMode = effectiveEmotion
        )
        val saved = repository.update(updated)
        if (updateNarratedOnly) {
            val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
            val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(id, sourceLang)
            if (translation != null) {
                storyTranslationRepository.save(
                    translation.copy(
                        content = content,
                        wordCount = wordCount,
                        readingTimeMinutes = readingTimeMinutes
                    )
                )
                log.info("Synced narrated content to translation for story id={} lang={}", id, sourceLang)
            }
        }
        return saved
    }

    /**
     * Search curated stories by theme or title (case-insensitive).
     * Language filter applied; for ta uses master, for others uses translation if available.
     */
    fun search(query: String, language: String, page: Int, size: Int): Page<CuratedStoryResponse> {
        if (query.isBlank()) return PageImpl(emptyList(), PageRequest.of(0, size.coerceIn(1, 50)), 0)
        val effectiveLang = effectiveLanguage(language)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 50))
        val results = repository.searchByThemeOrTitle(query, effectiveLang, pageable)
        return results.map {
            val coverPath = coverImageUrlResolver.resolveCoverPath(it.coverImageUrl)
            val audioUrl = resolvePlayableAudioUrl(it.id, effectiveLang)
            it.toResponse(coverPath, coverImageUrlResolver.resolveCoverVideoPath(it.coverVideoUrl))
                .copy(audioFileUrl = it.audioFileUrl?.takeIf { u -> u.isNotBlank() } ?: audioUrl)
        }
    }

    fun getPipelineStatusByStoryId(masterStoryId: Long): Map<String, String> {
        val story = repository.findById(masterStoryId) ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()

        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        val supportedLangs = (listOf(sourceLang) + appProperties.translationPipeline.targetLanguages
            .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }).distinct()

        for (lang in supportedLangs) {
            val translation = translations.find { it.language.equals(lang, ignoreCase = true) }
            // Source language (Tamil): story.audioFileUrl is authoritative—set when Tamil TTS completes.
            // Check it first so Tamil never reverts to PENDING when audio exists but translation lookup fails.
            val hasAudio = (lang == sourceLang && !story.audioFileUrl.isNullOrBlank()) ||
                resolveAudioFileUrl(masterStoryId, lang) != null
            // Reconcile: if audio exists but DB status is not COMPLETED, fix the DB.
            if (hasAudio && translation != null && translation.status != TranslationPipelineStatus.COMPLETED) {
                storyTranslationRepository.atomicStatusUpdate(translation.id, TranslationPipelineStatus.COMPLETED, null)
                log.info("Reconciled translation masterStoryId={} lang={}: status {} -> COMPLETED (audio exists)",
                    masterStoryId, lang, translation.status)
            }
            val ts = translation?.status?.name ?: "PENDING"
            // Prefer hasAudio as source of truth: if audio exists, language is COMPLETED.
            // Source language: story.audioFileUrl check first to avoid Tamil reverting to PENDING.
            val status = if (hasAudio) "COMPLETED" else ts
            val errorSuffix = if (translation?.lastError != null && translation.status?.isFailed() == true) {
                val retryInfo = if (translation.retryCount > 0) " (${translation.retryCount} retries)" else ""
                " — ${translation.lastError.take(250)}$retryInfo"
            } else ""
            result[lang] = status + errorSuffix
        }
        progressTracker.getProcessingLanguage(masterStoryId)?.let { processingLang ->
            result["processing"] = processingLang
        }
        return result
    }
}
