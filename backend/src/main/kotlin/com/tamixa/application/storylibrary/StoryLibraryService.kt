package com.tamixa.application.storylibrary

import com.tamixa.api.admin.LibraryStoryMapper.toResponse
import com.tamixa.api.admin.dto.LibraryStoryListingResponse
import com.tamixa.api.admin.dto.LibraryStoryResponse
import com.tamixa.api.admin.dto.TranslationContentEntryDto
import com.tamixa.application.narration.StoryProcessingService
import com.tamixa.application.port.StoryLibraryEventPublisherPort
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.OpenAIPort
import com.tamixa.application.port.TtsMetadataCachePort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationScriptRepositoryPort
import com.tamixa.application.story.ContentModerationException
import com.tamixa.application.story.ModerationContext
import com.tamixa.application.story.StoryModerationService
import com.tamixa.application.story.StoryPromptTemplates
import com.tamixa.application.story.StoryPromptBuilder
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.application.translation.TranslationService
import com.tamixa.infrastructure.persistence.FavoriteStoryJpaRepository
import com.tamixa.infrastructure.persistence.LibraryStoryLanguageReviewEntity
import com.tamixa.infrastructure.persistence.LibraryStoryLanguageReviewJpaRepository
import com.tamixa.infrastructure.persistence.StoryAnalyticsJpaRepository
import com.tamixa.infrastructure.persistence.StoryFeedbackJpaRepository
import com.tamixa.infrastructure.persistence.StoryPlaybackPositionJpaRepository
import com.tamixa.domain.TranslationPipelineStatus
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.domain.narration.StoryNarrationScript
import com.tamixa.domain.narration.ToneMode
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.StoryTranslation
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.observability.StoryPipelineMetrics
import com.tamixa.infrastructure.observability.PipelineProgressTracker
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import com.tamixa.application.library.LibraryStoryIllustrationService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.fasterxml.jackson.databind.ObjectMapper

@Service
class StoryLibraryService(
    private val repository: StoryLibraryRepositoryPort,
    private val eventPublisher: StoryLibraryEventPublisherPort,
    private val storyTranslationRepository: StoryTranslationRepositoryPort,
    private val openAI: OpenAIPort,
    private val storyPromptBuilder: StoryPromptBuilder,
    private val objectMapper: ObjectMapper,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val narrationScriptRepository: StoryNarrationScriptRepositoryPort,
    @Autowired(required = false) private val s3Client: S3Client?,
    @Autowired(required = false) private val ttsMetadataCache: TtsMetadataCachePort?,
    private val favoriteStoryJpaRepository: FavoriteStoryJpaRepository,
    private val storyAnalyticsJpaRepository: StoryAnalyticsJpaRepository,
    private val storyPlaybackPositionJpaRepository: StoryPlaybackPositionJpaRepository,
    private val storyFeedbackJpaRepository: StoryFeedbackJpaRepository,
    private val libraryStoryLanguageReviewJpaRepository: LibraryStoryLanguageReviewJpaRepository,
    private val appProperties: AppProperties,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    private val progressTracker: PipelineProgressTracker,
    @Qualifier("triggerPipelineExecutor") private val triggerPipelineExecutor: TaskExecutor,
    @Lazy private val storyProcessingService: StoryProcessingService,
    private val storyModeration: StoryModerationService,
    private val translationService: TranslationService,
    @Lazy private val libraryStoryIllustrationService: LibraryStoryIllustrationService,
    private val storyPipelineMetrics: StoryPipelineMetrics,
    /** Resolves this service lazily so [applyAdminTranslationEditsInNewTransaction] runs through the Spring proxy (REQUIRES_NEW). */
    private val storyLibrarySelf: ObjectProvider<StoryLibraryService>
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val updateSlowWarnMs = 10_000L
    private val updateSlowErrorMs = 20_000L

    companion object {
        private val languageAliases = mapOf(
            "tamil" to "ta", "english" to "en", "hindi" to "hi",
            "telugu" to "te", "kannada" to "kn", "malayalam" to "ml", "bengali" to "bn"
        )
        /** Tamil Unicode U+0B80–U+0BFF. When serving non-Tamil, reject title/moral that contain Tamil (wrong-language data). */
        private val TAMIL_SCRIPT = Regex("[\u0B80-\u0BFF]")
        /** Devanagari (Hindi, etc.) U+0900–U+097F. */
        private val DEVANAGARI_SCRIPT = Regex("[\u0900-\u097F]")
        /** Telugu U+0C00–U+0C7F. */
        private val TELUGU_SCRIPT = Regex("[\u0C00-\u0C7F]")
        /** Kannada U+0C80–U+0CFF. */
        private val KANNADA_SCRIPT = Regex("[\u0C80-\u0CFF]")
        /** Malayalam U+0D00–U+0D7F. */
        private val MALAYALAM_SCRIPT = Regex("[\u0D00-\u0D7F]")
        /** Script-to-language mapping for content-based language inference. Check in order; first match wins. */
        private val SCRIPT_TO_LANG = listOf(
            TAMIL_SCRIPT to "ta",
            MALAYALAM_SCRIPT to "ml",
            KANNADA_SCRIPT to "kn",
            TELUGU_SCRIPT to "te",
            DEVANAGARI_SCRIPT to "hi"
        )
    }

    /** Infers language from content script when a supported Indic script is detected. Returns null for English or ambiguous. */
    private fun inferLanguageFromScript(content: String): String? {
        if (content.isBlank()) return null
        for ((script, lang) in SCRIPT_TO_LANG) {
            if (script.containsMatchIn(content)) return lang
        }
        return null
    }

    private fun rejectIfTamilWhenNotTa(text: String?, language: String): String? {
        if (text.isNullOrBlank()) return text
        if (language.trim().lowercase() == "ta") return text
        return if (TAMIL_SCRIPT.containsMatchIn(text)) {
            log.debug("Serving lang={}: title/moral contained Tamil script (wrong-language data), returning empty", language)
            null
        } else text
    }

    private fun effectiveLanguage(language: String): String {
        val normalized = language.trim().lowercase()
        return languageAliases[normalized] ?: normalized
    }

    /**
     * Requested locale matches configured [com.tamixa.infrastructure.config.AppProperties.TranslationPipelineProperties.sourceLanguage]:
     * list/search themes use `library_stories` as canonical rows instead of `story_translations`.
     */
    private fun isPrimaryCatalogLanguage(effectiveLang: String): Boolean {
        val primary = appProperties.translationPipeline.sourceLanguage.trim().lowercase().take(10)
        return effectiveLang.equals(primary, ignoreCase = true)
    }

    /** Sanitize pipeline error for admin display; avoid leaking paths or internal details. */
    private fun sanitizeErrorForDisplay(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = raw.trim().take(250)
        // Redact absolute file paths
        s = Regex("""(?:/[\w.-]+){2,}""").replace(s) { if (it.value.length > 30) "[path redacted]" else it.value }
        s = Regex("""[A-Za-z]:\\[^\s]{20,}""").replace(s) { "[path redacted]" }
        return s.take(250)
    }

    private fun normalizeForCompare(value: String?): String =
        value?.trim()?.replace(Regex("\\s+"), " ") ?: ""

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { b -> "%02x".format(b.toInt() and 0xff) }
    }

    /**
     * Stable fingerprint of editable text for a language (title, body, moral, narration script).
     * For the **pipeline source** language, hashes **both** library master and `story_translations` row when present:
     * draft saves only update `library_stories` (translation sync runs on PUBLISHED), so using translation alone
     * missed Tamil edits and kept review badges green.
     * Other languages use the translation row only.
     */
    private fun fingerprintLanguageContent(
        story: LibraryStory,
        languageLower: String,
        pipelineSourceLangLower: String,
        translations: List<StoryTranslation>
    ): String {
        val lang = languageLower.trim().lowercase()
        val source = pipelineSourceLangLower.trim().lowercase()
        val isSource = lang == source
        val t = translations.find { it.language.trim().lowercase() == lang }
        val scriptText = t?.let { narrationScriptRepository.findByTranslationId(it.id)?.scriptText }
        val raw = if (isSource) {
            val masterBundle = listOf(
                normalizeForCompare(story.title),
                normalizeForCompare(story.content),
                normalizeForCompare(story.moral)
            ).joinToString("\u001e")
            val transBundle = if (t != null) {
                listOf(
                    normalizeForCompare(t.title),
                    normalizeForCompare(t.content),
                    normalizeForCompare(t.moral)
                ).joinToString("\u001e")
            } else {
                ""
            }
            listOf(masterBundle, transBundle, normalizeForCompare(scriptText)).joinToString("\u001f")
        } else {
            val title: String?
            val content: String
            val moral: String?
            if (t != null) {
                title = t.title
                content = t.content
                moral = t.moral
            } else {
                title = null
                content = ""
                moral = null
            }
            listOf(
                normalizeForCompare(title),
                normalizeForCompare(content),
                normalizeForCompare(moral),
                normalizeForCompare(scriptText)
            ).joinToString("\u001e")
        }
        return sha256Hex(raw.toByteArray(StandardCharsets.UTF_8))
    }

    private fun hasTranslationContentChanged(
        existing: StoryTranslation?,
        incomingContent: String,
        incomingTitle: String?,
        incomingMoral: String?
    ): Boolean {
        if (existing == null) return true
        return normalizeForCompare(existing.content) != normalizeForCompare(incomingContent) ||
            normalizeForCompare(existing.title) != normalizeForCompare(incomingTitle) ||
            normalizeForCompare(existing.moral) != normalizeForCompare(incomingMoral)
    }

    /**
     * Resolves audio URL for display from story_narration_audio.
     * Used when library story audio_file_url is null but pipeline has stored audio.
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
     * When [com.tamixa.infrastructure.config.AppProperties.TranslationPipelineProperties.masterOnlyNarration] is true,
     * audio is always taken from the story's master language row (or legacy master `audio_file_url`), regardless of [requestedLanguage].
     */
    private fun audioLanguageForPlayback(masterStoryId: Long, requestedLanguage: String): String {
        val req = requestedLanguage.trim().lowercase().take(10)
        if (!appProperties.translationPipeline.masterOnlyNarration) return req
        val master = repository.findById(masterStoryId) ?: return req
        return master.language.trim().lowercase().take(10).ifEmpty { req }
    }

    /**
     * Resolves canonical audio path from story_narration_audio (and legacy master `audio_file_url` when applicable).
     * Ensures mobile and admin always play the same narrated audio.
     */
    private fun resolveAudioFileUrl(masterStoryId: Long, language: String): String? {
        val audioLang = audioLanguageForPlayback(masterStoryId, language)
        val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(masterStoryId, audioLang)
        if (translation != null) {
            // Prefer latest READY default-voice row. Some stories can have historical FAILED rows for the same
            // (translationId, voiceProfile), and fetching a single row can incorrectly return a stale non-READY entry.
            val narration = narrationAudioRepository.findAllByTranslationId(translation.id)
                .asSequence()
                .filter { it.voiceProfile == "default" }
                .filter { it.status == NarrationAudioStatus.READY && it.audioUrl.isNotBlank() }
                .maxByOrNull { it.createdAt }
            if (narration != null) return narration.audioUrl
        }
        // Legacy master column: only when serving master-language audio for all locales (see masterOnlyNarration).
        // When flag is off, behavior unchanged: no narration on the requested row ⇒ null (callers add ta-specific fallbacks).
        if (!appProperties.translationPipeline.masterOnlyNarration) return null
        val master = repository.findById(masterStoryId) ?: return null
        val masterLang = master.language.trim().lowercase().take(10)
        if (masterLang.isNotEmpty() && audioLang.equals(masterLang, ignoreCase = true)) {
            return master.audioFileUrl?.takeIf { it.isNotBlank() }
        }
        return null
    }

    /**
     * Returns playable audio URL for mobile: full URL when publicBaseUrl is set, else raw path.
     * Narration paths (stories/...) become {base}/audio/{path}?v={createdAt} for cache busting.
     */
    private fun resolvePlayableAudioUrl(masterStoryId: Long, language: String): String? {
        val path = resolveAudioFileUrl(masterStoryId, language) ?: return null
        val audioLang = audioLanguageForPlayback(masterStoryId, language)
        return pathToPlayableUrl(path, masterStoryId, audioLang)
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
     * Create library story from text. Audio is generated asynchronously via TTS pipeline.
     * Admin uploads text only; system generates audio automatically.
     * Validates: min word count, Tamil script (for ta), duplicate title, category required.
     */
    @Transactional
    fun create(
        title: String?,
        content: String,
        theme: String,
        category: String? = null,
        language: String = "ta",
        age: Int,
        childName: String = "Child",
        moral: String?,
        audioFileUrl: String? = null,  // Ignored; system generates audio from text
        status: String = "DRAFT",
        coverImageUrl: String? = null,
        emotionMode: String? = null,
        storyOwner: String? = null,
        convertPromptUsed: String? = null,
        /** When set (e.g. from bulk JSON estimated_duration), used instead of word-count-derived reading time. */
        estimatedReadingMinutes: Double? = null,
        parentDiscussionPrompts: List<String>? = null,
        parentContentNote: String? = null,
        speakAlongPrompt: String? = null,
    ): LibraryStory {
        // Validation: category/theme required (enforced by @NotBlank on theme)
        // Min word count
        StoryLibraryValidation.validateMinWordCount(content).getOrElse { throw it }
        // Script validation: ensure content matches selected language (bulk generator + manual create)
        StoryLibraryValidation.validateScriptForLanguage(content, language).getOrElse { throw it }
        // Duplicate title (when title provided)
        title?.takeIf { it.isNotBlank() }?.let { t ->
            if (repository.existsByTitle(t)) {
                throw IllegalArgumentException("A story with this title already exists")
            }
        }

        val wordCount = content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val readingTimeMinutes = (estimatedReadingMinutes?.coerceIn(0.5, 30.0))
            ?: (wordCount / 150.0).coerceIn(1.0, 30.0)
        val now = java.time.Instant.now()
        val effectiveStatus = status?.take(20)?.uppercase()?.let { if (it in listOf("DRAFT", "PUBLISHED")) it else "DRAFT" } ?: "DRAFT"
        val effectiveEmotion = emotionMode?.trim()?.uppercase()?.take(20)
            ?.let { if (it in listOf("CALM", "SOOTHING", "ADVENTUROUS")) it else "CALM" } ?: "CALM"
        val prompts = parentDiscussionPrompts
            ?.mapNotNull { it.trim().takeIf { s -> s.isNotBlank() }?.take(400) }
            ?.take(10)
            ?.takeIf { it.isNotEmpty() }
        val story = LibraryStory(
            id = 0,
            title = title,
            content = content,
            theme = theme,
            category = category?.trim()?.take(100) ?: theme,
            language = language.trim().lowercase().take(10).ifEmpty { "ta" },
            age = age.coerceIn(1, 99),
            childName = childName.ifBlank { "Child" }.take(255),
            wordCount = wordCount,
            readingTimeMinutes = readingTimeMinutes,
            moral = moral,
            audioFileUrl = null,  // System generates via TTS pipeline
            status = effectiveStatus,
            coverImageUrl = coverImageUrl?.take(512),
            createdAt = now,
            updatedAt = now,
            storyOwner = storyOwner?.trim()?.takeIf { it.isNotBlank() }?.take(255),
            convertPromptUsed = convertPromptUsed?.trim()?.takeIf { it.isNotBlank() }?.take(8000),
            emotionMode = effectiveEmotion,
            narrationApprovedAt = null,
            deletedAt = null,
            parentDiscussionPrompts = prompts,
            parentContentNote = parentContentNote?.trim()?.takeIf { it.isNotBlank() }?.take(4000),
            speakAlongPrompt = speakAlongPrompt?.trim()?.takeIf { it.isNotBlank() }?.take(500),
        )
        val saved = repository.save(story)
        // Create initial story_translation row for the story's language so content is visible in admin (all languages) and pipeline has a row to update
        val initialTranslation = StoryTranslation(
            id = 0,
            masterStoryId = saved.id,
            language = saved.language,
            title = saved.title,
            content = saved.content,
            moral = saved.moral,
            wordCount = saved.wordCount,
            readingTimeMinutes = saved.readingTimeMinutes,
            createdAt = now,
            status = TranslationPipelineStatus.PENDING,
            retryCount = 0,
            lastError = null,
            narrationApprovedAt = null
        )
        storyTranslationRepository.save(initialTranslation)
        eventPublisher.publishLibraryStoryCreated(saved.id, saved.content, saved.status)
        return saved
    }

    fun findById(id: Long): LibraryStory? = repository.findById(id)

    /**
     * Maintenance normalization:
     * if narration has been approved, master status should be PUBLISHED.
     */
    @Transactional
    fun normalizeMasterStatusesForApprovedStories(pageSize: Int = 200): Map<String, Any> {
        var page = 0
        var scanned = 0
        var changed = 0
        val changedIds = mutableListOf<Long>()
        while (true) {
            val result = repository.findAll(PageRequest.of(page, pageSize.coerceIn(20, 500)))
            scanned += result.numberOfElements
            result.content.forEach { story ->
                if (story.narrationApprovedAt != null && story.status != StoryStatus.PUBLISHED) {
                    repository.updateStatus(story.id, StoryStatus.PUBLISHED)
                    changed++
                    changedIds += story.id
                }
            }
            if (result.isLast) break
            page++
        }
        return mapOf(
            "scanned" to scanned,
            "changed" to changed,
            "changedIds" to changedIds
        )
    }

    /**
     * Resolves language for admin edit form when no language param is given.
     * When master content has an Indic script that differs from story.language (e.g. bulk-generated Hindi story
     * edited to Tamil, or vice versa), return the inferred language so edit form loads the correct content.
     */
    fun resolveLanguageForAdminEdit(story: LibraryStory): String {
        val storyLang = story.language.trim().lowercase().take(10).ifEmpty { "ta" }
        val content = story.content?.trim().orEmpty()
        val inferred = inferLanguageFromScript(content)
        return if (inferred != null && inferred != storyLang) {
            log.debug("Admin edit story id={}: content has {} script, resolving language {} -> {}", story.id, inferred, storyLang, inferred)
            inferred
        } else storyLang
    }

    /**
     * After bulk create (or any master in [masterLanguage]), pre-fill [StoryTranslation] rows for all pipeline
     * target languages so the admin sees every language without waiting for the narration pipeline.
     * Uses the master's actual language as the translation source—not only [AppProperties.translationPipeline.sourceLanguage],
     * so env overrides (e.g. EN source) cannot mismatch Tamil/Hindi masters and yield empty or wrong translations.
     */
    private fun seedTranslationsForPipelineTargets(
        masterStoryId: Long,
        masterLanguage: String,
        title: String?,
        content: String,
        moral: String?
    ) {
        if (appProperties.translationPipeline.masterOnlyNarration) {
            log.debug("Skipping bulk translation seed masterStoryId={} (master-only-narration)", masterStoryId)
            return
        }
        val sourceLang = masterLanguage.trim().lowercase().take(10).ifBlank {
            appProperties.translationPipeline.sourceLanguage.trim().lowercase()
        }
        val targets = appProperties.translationPipeline.targetLanguages
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() && it != sourceLang }
        if (targets.isEmpty()) return
        val now = java.time.Instant.now()
        for (targetLang in targets) {
            try {
                if (storyTranslationRepository.existsByMasterStoryIdAndLanguage(masterStoryId, targetLang)) {
                    continue
                }
                val result = translationService.translateIfNeeded(
                    sourceLang = sourceLang,
                    targetLang = targetLang,
                    title = title,
                    content = content,
                    moral = moral
                )
                if (result.content.isBlank()) {
                    log.warn("Bulk seed translation returned empty content masterStoryId={} lang={}", masterStoryId, targetLang)
                    continue
                }
                val wordCount = result.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
                val readingTimeMinutes = (wordCount / 150.0).coerceIn(1.0, 30.0)
                storyTranslationRepository.save(
                    StoryTranslation(
                        id = 0,
                        masterStoryId = masterStoryId,
                        language = targetLang,
                        title = result.title,
                        content = result.content.take(50_000),
                        moral = result.moral?.take(2000),
                        wordCount = wordCount,
                        readingTimeMinutes = readingTimeMinutes,
                        createdAt = now,
                        status = TranslationPipelineStatus.PENDING,
                        retryCount = 0,
                        lastError = null,
                        narrationApprovedAt = null
                    )
                )
                log.debug("Bulk seed translation saved masterStoryId={} lang={}", masterStoryId, targetLang)
            } catch (e: Exception) {
                log.warn(
                    "Bulk seed translation failed masterStoryId={} lang={}: {}",
                    masterStoryId,
                    targetLang,
                    e.message
                )
            }
        }
    }

    /**
     * @param progressCallback When set (e.g. for async jobs), called after each story with current index and result so far.
     */
    fun generateBulkStoriesWithTemplatePrompt(
        languages: List<String>,
        categories: List<String>,
        totalStories: Int,
        publish: Boolean,
        storyOwner: String?,
        /** Optional; same allowlist as parent generate; woven into bulk template via [StoryPromptBuilder.learningFocusLineForBulk]. */
        learningFocus: String? = null,
        progressCallback: ((currentIndex: Int, total: Int, createdCount: Int, failedCount: Int, created: List<Map<String, Any?>>, failed: List<Map<String, Any?>>) -> Unit)? = null
    ): Map<String, Any> {
        // Always Tamil masters (pipeline source). Request `languages` is ignored; all other app languages are filled via translation seed below.
        languages.map { effectiveLanguage(it) }.distinct().filter { it.isNotBlank() && it != "ta" }.takeIf { it.isNotEmpty() }?.let {
            log.debug("Bulk generation: ignoring requested languages {} (masters are always Tamil)", it)
        }
        val safeLanguages = listOf("ta")
        val safeCategories = StoryCategories.normalize(categories)
            .ifEmpty { StoryCategories.canonical }
        val requestedTotal = totalStories.coerceIn(1, 25)

        val status = if (publish) StoryStatus.PUBLISHED else StoryStatus.DRAFT
        val created = mutableListOf<Map<String, Any?>>()
        val failed = mutableListOf<Map<String, Any?>>()
        var totalRequested = 0
        val combinations = mutableListOf<Pair<String, String>>()
        safeLanguages.forEach { lang ->
            safeCategories.forEach { category ->
                combinations.add(lang to category)
            }
        }

        repeat(requestedTotal) { idx ->
            totalRequested++
            val (language, category) = combinations[idx % combinations.size]
            log.info("Bulk story generation {}/{} lang={} category={}", idx + 1, requestedTotal, language, category)
            val prompt = buildBulkGenerationPrompt(language, category, idx, learningFocus)
            try {
                val raw = openAI.generateStory(prompt, 2048)
                val json = objectMapper.readTree(raw)
                var title = json.path("title").asText("").trim().ifBlank { "$category Story ${idx + 1}" }
                val themeFromJson = json.path("theme").asText("").trim().ifBlank { category }
                // Use the requested category (what we sent in the prompt) so saved stories match the user's selection; do not use AI-returned category which may be translated or wrong.
                val categoryToSave = category.take(100)
                val storyText = extractStoryTextFromBulkJson(json)
                val moral = json.path("moral").asText("").trim().ifBlank { null }
                if (storyText.isBlank()) throw IllegalArgumentException("Model returned empty story_text")
                val estimatedMinutes = parseEstimatedMinutesFromBulkJson(json)

                storyModeration.moderateBeforeSave(
                    storyText,
                    ModerationContext(promptId = "bulk-$idx", language = language, age = 7)
                )

                val saved = try {
                    create(
                        title = title.take(255),
                        content = storyText.take(50_000),
                        theme = themeFromJson.take(100),
                        category = categoryToSave,
                        language = language,
                        age = 7,
                        childName = "Child",
                        moral = moral?.take(500),
                        status = status,
                        storyOwner = storyOwner,
                        convertPromptUsed = prompt,
                        estimatedReadingMinutes = estimatedMinutes
                    )
                } catch (e: IllegalArgumentException) {
                    if (e.message?.contains("already exists") == true && title.isNotBlank()) {
                        val uniqueTitle = "${title.take(250)} (${idx + 1})"
                        create(
                            title = uniqueTitle,
                            content = storyText.take(50_000),
                            theme = themeFromJson.take(100),
                            category = categoryToSave,
                            language = language,
                            age = 7,
                            childName = "Child",
                            moral = moral?.take(500),
                            status = status,
                            storyOwner = storyOwner,
                            convertPromptUsed = prompt,
                            estimatedReadingMinutes = estimatedMinutes
                        )
                    } else throw e
                }
                seedTranslationsForPipelineTargets(saved.id, saved.language, saved.title, saved.content, saved.moral)
                created.add(
                    mapOf(
                        "id" to saved.id,
                        "language" to language,
                        "category" to categoryToSave,
                        "title" to (saved.title ?: "")
                    )
                )
            } catch (e: ContentModerationException) {
                log.warn("Bulk story rejected by moderation idx={} lang={} category={}: {}", idx, language, category, e.message)
                failed.add(
                    mapOf(
                        "language" to language,
                        "category" to category,
                        "error" to ("content_moderation: " + (e.message?.take(200) ?: "rejected"))
                    )
                )
            } catch (e: Exception) {
                failed.add(
                    mapOf(
                        "language" to language,
                        "category" to category,
                        "error" to (e.message ?: "generation_failed")
                    )
                )
            }
            progressCallback?.invoke(idx + 1, requestedTotal, created.size, failed.size, created.toList(), failed.toList())
        }

        return mapOf(
            "requested" to totalRequested,
            "createdCount" to created.size,
            "failedCount" to failed.size,
            "publish" to publish,
            "created" to created,
            "failed" to failed
        )
    }

    /** Language code to display name and explicit instruction so the model outputs in the correct language with that language's own grammar and style. */
    private fun bulkPromptLanguageInstruction(code: String): String {
        val normalized = code.trim().lowercase()
        val name = when (normalized) {
            "ta" -> "Tamil"
            "en" -> "English"
            "hi" -> "Hindi"
            "te" -> "Telugu"
            "kn" -> "Kannada"
            "ml" -> "Malayalam"
            else -> code
        }
        val base = "CRITICAL — Output language: $name (code: $normalized). You MUST write this story entirely in $name. Every field—title, category, theme, story_text, moral—must be in $name only. Do not mix languages. Use correct $name grammar, vocabulary, and that language's own natural style as used by native speakers."
        val grammarStyleAndVocab = when (normalized) {
            "ta" -> " Tamil: use Tamil script (Unicode); proper verb forms (past/present/future suffixes), subject-object-verb order. Vocabulary: use everyday Tamil words and native terms; prefer Tamil words over English loanwords where natural; age-appropriate, simple words for children; avoid literal English translation. Use correct pronouns and number agreement."
            "en" -> " English: use standard English grammar (subject-verb-object), correct tense and agreement. Vocabulary: clear, everyday words; age-appropriate and easy to narrate; natural conversational storytelling."
            "hi" -> " Hindi: use Devanagari script; correct Hindi verb conjugation and postpositions; natural word order and honorifics. Vocabulary: common Hindi words and expressions; prefer native Hindi vocabulary where natural; age-appropriate; conversational audiobook style."
            "te" -> " Telugu: use Telugu script; correct Telugu verb forms and case suffixes; natural SOV order. Vocabulary: everyday Telugu words; native terms and expressions; age-appropriate; write as a native Telugu speaker would tell a story."
            "kn" -> " Kannada: use Kannada script; correct Kannada verb conjugation and case markers; natural word order. Vocabulary: common Kannada words; native expressions; age-appropriate; natural storytelling rhythm."
            "ml" -> " Malayalam: use Malayalam script; correct verb forms and agglutination; natural word order. Vocabulary: everyday Malayalam words; native vocabulary; age-appropriate; avoid literal translation from other languages."
            else -> ""
        }
        return base + grammarStyleAndVocab
    }

    /** Extract story_text from bulk-generation JSON; supports root or nested (e.g. data.story_text) so content is not lost. */
    private fun extractStoryTextFromBulkJson(json: com.fasterxml.jackson.databind.JsonNode): String {
        val atRoot = json.path("story_text").asText("").trim()
        if (atRoot.isNotBlank()) return atRoot
        val fromData = json.path("data").path("story_text").asText("").trim()
        if (fromData.isNotBlank()) return fromData
        val fromStory = json.path("story").path("story_text").asText("").trim()
        if (fromStory.isNotBlank()) return fromStory
        return ""
    }

    /**
     * Parses estimated narration duration from bulk JSON into minutes.
     * Accepts: number (seconds or minutes), or string like "10 min", "8 minutes", "600" (seconds).
     */
    private fun parseEstimatedMinutesFromBulkJson(json: com.fasterxml.jackson.databind.JsonNode): Double? {
        val secondsNode = json.path("estimated_duration_seconds")
        if (!secondsNode.isMissingNode && secondsNode.isNumber) {
            val sec = secondsNode.asDouble()
            if (sec > 0) return (sec / 60.0).coerceIn(0.5, 30.0)
        }
        val raw = json.path("estimated_duration").asText("").trim()
        if (raw.isBlank()) return null
        val numMatch = Regex("""(\d+(?:\.\d+)?)""").find(raw) ?: return null
        val value = numMatch.groupValues[1].toDoubleOrNull() ?: return null
        if (value <= 0) return null
        val isSeconds = raw.contains("sec", ignoreCase = true) || (!raw.contains("min", ignoreCase = true) && value >= 60)
        return if (isSeconds) (value / 60.0).coerceIn(0.5, 30.0) else value.coerceIn(0.5, 30.0)
    }

    private fun bulkLearningFocusSection(learningFocus: String?): String {
        val line = storyPromptBuilder.learningFocusLineForBulk(learningFocus)
        if (line.isBlank()) return ""
        return """
## Editor-selected learning focus (apply to every story in this request)
$line
Integrate this emphasis clearly in plot and dialogue in addition to the general educational guidelines above; do not contradict safety, cultural, or tone rules.

""".trimIndent()
    }

    private fun buildBulkGenerationPrompt(language: String, category: String, index: Int, learningFocus: String? = null): String {
        val combinedSituation = combinedSituations[index % combinedSituations.size]
        val languageInstruction = bulkPromptLanguageInstruction(language)
        val langReminder = "Remember: write the entire story and all JSON string fields in the output language specified at the top—no mixing of languages."
        val focusSection = bulkLearningFocusSection(learningFocus)
        val basePrompt = """
$languageInstruction

## Role & mandate
You are Tamixa's story designer. Generate one publication-ready story for the mobile storytelling app. Quality and safety are non-negotiable. Every story must be suitable for TTS, family listening, and Indian family-viewing standards.

## Primary structure & clarity (vocabulary is the top priority)
English is the structural reference. When the output language is not English, use the same sentence clarity (short, clear sentences; one idea per sentence; clear subject and action), then write in the target language so every sentence stays clear and easy to narrate.
• **Vocabulary first:** prefer simple, everyday words a child listener already knows in the output language. Clarity beats sounding literary; avoid rare or textbook-heavy phrasing. If you use a less common word, make its meaning obvious from context in one hearing.
• One idea per sentence. Clear subject and verb. Short to medium length. No run-ons. Natural connectors (Then… So… But… or equivalent). Every sentence easy to read aloud in one pass.
• Warm storytelling tone; no jargon. Sound like natural spoken storytelling in the output language—not literal translation phrasing or stiff textbook style.

## Continuity & narrative flow (mandatory — no gaps, no missing scenes)
• One clear timeline from opening to ending; cause and effect must be easy to follow. When time or place changes, use an explicit transition in the output language (next day, after a while, meanwhile, or natural equivalents)—never jump without orienting the listener.
• **No missing scenes:** do not skip beats or leave resolutions off-stage. Every narrative step the listener needs must appear on the page—problem, attempts, emotional turns, and resolution—with no unexplained jumps.
• No plot gaps: do not leap from problem to resolution without the key steps the story needs. Every important character or thread is introduced, developed, and closed or clearly handed off.
• Dialogue and narration must flow together: each block follows logically from the previous; no abrupt scene cuts or missing beats.

Category: {category}
Combined Situation: {combinedSituation}

## Quality bar
• Publication-ready: prose that would pass editorial review. Intellectually rich: thought-provoking, meaningful; moral that feels earned.
• Mild conflict resolved peacefully; positive, satisfying ending. Title: catchy and specific (not generic like "A Friendship Story"). Moral: one short sentence; positive value; age-appropriate; not preachy.
• Appeal to both children and adults—engaging for kids, satisfying and thoughtful for elders. Layered meaning, subtle lessons (kindness, courage, honesty, belonging).

## Tone & voice (Tamixa voice)
• Magical, comforting, and joyful. Warm, friendly, emotionally gentle. Like a trusted parent, teacher, or grandparent. No sarcasm, cynicism, or fear.
• Narration: short blocks (1–3 sentences); natural transitions ("Once upon a time…", "One day…", "Then…", "After that…" or equivalent). Dialogue simple and sparse; every sentence natural when read aloud.

## Educational perspective (Tamixa differentiator — build learning into every story)
• Build learning naturally into the plot; do not lecture. Competitors focus on entertainment only; Tamixa stories support cognitive, social-emotional, and language growth.
• Age band for this bulk prompt: assume approx. age ~7 (use ages 5–7 guidance).
• Age-banded learning design (ages 5–7): empathy and naming feelings, simple problem-solving, sequencing (first, then, last), include 2–3 "wonder words" in context.
• SEL (social-emotional learning): include 1–2 situations where characters name their feelings (happy, worried, brave, kind, proud, grateful) in the output language. Show kind or brave choices; resolve conflict through talking and understanding, not force.
• Wonder words: weave in 2–4 age-appropriate new or rich words used clearly in context. Use each word at least twice so the listener can infer meaning. No glossary needed—meaning clear from the story.
• Inference-friendly narrative: include at least one moment that rewards paying attention—e.g. a character's choice that makes sense given what happened earlier, or a gentle "what might happen next?" beat. No quizzes; the story itself should invite thinking. Avoid spelling everything out; allow the child to connect dots.
• One clear problem, one satisfying resolution: one main challenge per story. Resolution must be earned (character effort, help from others, or a lesson learned). Supports comprehension and gives a clear takeaway.
• NEP / life skills alignment: support critical thinking (characters weighing options), collaboration (helping each other), cultural awareness (respect, diversity), and values (honesty, sharing, courage) implicitly in the plot, not preachy.

{bulkLearningFocusSection}
## Storytelling Script format (story_text)
• Narrator + dialogue. Use "Character: dialogue text" or clear attribution. MUST include inline markers throughout: [Pause 500ms], [Pause 1s], [Happy tone], [Soft voice], [Warm tone], [Calm], [Whisper], [Excited]. Use them at natural break points, before/after dialogue, and at emotional beats. SSML/OpenAI TTS ready. Same requirement for every language.

## Cultural context (Indian)
• Draw on Indian folklore and mythology where appropriate: Tenali Rama–style wit and wisdom, Panchatantra-style animal tales and morals, regional folktales. Use festival themes in the output language when they fit—e.g. Diwali, Pongal, Onam, Ugadi—with names, customs, and terms in the native language (Tamil, Hindi, Telugu, Kannada, Malayalam). Keep retellings family-friendly and respectful of all communities.
• Prefer Indian village / town / nature settings. Culturally familiar names and values: kindness, friendship, honesty, courage, respect for elders. Stories should feel rooted in Indian cultural soil while inclusive for all audiences.

## Security & compliance (mandatory — zero tolerance)
• Child safety: Suitable for minors (including under 12). No violence, gore, horror, abuse, fear-inducing or distressing scenes. No dangerous or easily imitable behaviour. No self-harm, suicide, or substance use (drugs, alcohol).
• Indian compliance: Align with accepted Indian standards for children's storytelling and family viewing. Do not disparage or stereotype any religion, community, region, or language. No political messaging. No content that could be deemed hateful, discriminatory, or inflammatory under Indian norms.
• Positive values only: Respect for elders, friendship, honesty, sharing, inclusivity. Conflicts resolved peacefully. No bullying, humiliation, or negative stereotyping.
• Never include: violence, weapons, death, war, politics, religious conflict, drugs, alcohol, self-harm, advertising, brand names, real celebrities, real sensitive places, adult or romantic themes. Story must be purely fictional and uplifting.

## Length & output (target ~7–8 minutes TTS)
• Aim for **about 7–8 minutes** of narration: typically **~${StoryPromptTemplates.FULL_LENGTH_WORDS_MIN}–${StoryPromptTemplates.FULL_LENGTH_WORDS_MAX} words** of speakable content in the output language (excluding TTS marker tokens), at a moderate, clear pace for children.
• Set **estimated_duration_seconds** in JSON to match (e.g. **~${StoryPromptTemplates.FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MIN}–${StoryPromptTemplates.FULL_LENGTH_ESTIMATED_DURATION_SECONDS_MAX} seconds**)—must be consistent with story length.
$langReminder
## Output contract (strict)
Return only a single valid JSON object. No markdown, no code fence, no explanatory text.
Keys exactly: "title", "category", "theme", "moral", "story_text", "estimated_duration_seconds".
- title: Catchy, specific to the story; reflects theme or main idea. Not generic.
- category: One of the standard categories that best fits the story.
- theme: Short theme phrase in the story's language.
- story_text: Full narrative in Storytelling Script format (narrator + dialogue) with tone markers; magical, comforting, joyful; SSML/OpenAI TTS ready.
- moral: One short sentence; positive value; age-appropriate; earned by the story.
- estimated_duration_seconds: Number only (e.g. 420 for ~7 minutes).
Before responding: confirm no prohibited content; confirm language and grammar; confirm JSON is valid and complete.

{
"title": "",
"category": "",
"theme": "",
"story_text": "",
"moral": "",
"estimated_duration_seconds": 0
}
""".trimIndent()
        return basePrompt
            .replace("{bulkLearningFocusSection}", focusSection)
            .replace("{category}", category)
            .replace("{combinedSituation}", combinedSituation)
    }

    private val combinedSituations = listOf(
        "A village festival day mixed with a riverside nature walk and helping neighbors",
        "A small town school event mixed with a home garden challenge and friendship misunderstanding",
        "A farm morning mixed with a temple street celebration and family teamwork",
        "A rainy day in town mixed with a village market visit and caring for animals",
        "A hill-side picnic mixed with a local library activity and learning honesty",
        "A train trip to grandparents mixed with a town park clean-up and kindness to strangers",
        "A forest-edge trail mixed with a kite festival and courage to speak the truth",
        "A beach sunrise mixed with a craft fair in town and sharing responsibility",
        "A harvest field visit mixed with a storytelling night and helping younger children",
        "A community cooking day mixed with a village sports game and respectful teamwork"
    )

    /**
     * Admin editors should show the conversational script only when the pipeline has moved past rewrite
     * and the row is not re-queued (e.g. after submit-for-review invalidation → PENDING → show source text).
     */
    private fun preferNarratedScriptForAdminEditor(
        scriptText: String?,
        translationStatus: TranslationPipelineStatus?
    ): Boolean {
        if (scriptText.isNullOrBlank()) return false
        val st = translationStatus ?: return false
        return when (st) {
            TranslationPipelineStatus.PENDING,
            TranslationPipelineStatus.TRANSLATING,
            TranslationPipelineStatus.TRANSLATION_FAILED,
            TranslationPipelineStatus.REWRITING,
            TranslationPipelineStatus.REWRITE_FAILED -> false
            else -> true
        }
    }

    fun findByIdAndLanguage(id: Long, language: String): LibraryStoryResponse? {
        val effectiveLang = effectiveLanguage(language)
        val master = repository.findById(id) ?: return null
        val masterLang = master.language.trim().lowercase().take(10).ifEmpty {
            appProperties.translationPipeline.sourceLanguage.trim().lowercase().take(10)
        }
        return when {
            effectiveLang.equals(masterLang, ignoreCase = true) -> {
                val masterTranslation = storyTranslationRepository.findByMasterStoryIdAndLanguage(id, effectiveLang)
                val narratedContent = masterTranslation?.let { narrationScriptRepository.findByTranslationId(it.id)?.scriptText?.takeIf { it.isNotBlank() } }
                // Prefer translation row, then master (canonical after edits), then pipeline script —
                // old narrated script must not mask freshly submitted master/translation text.
                val displayContent = masterTranslation?.content?.takeIf { it.isNotBlank() }
                    ?: master.content.takeIf { it.isNotBlank() }
                    ?: narratedContent
                    ?: ""
                val sourceContent = masterTranslation?.content?.takeIf { it.isNotBlank() }
                    ?: master.content.takeIf { it.isNotBlank() }
                    ?: ""
                val preferNarrated = preferNarratedScriptForAdminEditor(narratedContent, masterTranslation?.status)
                val displayTitle = masterTranslation?.title?.takeIf { it.isNotBlank() } ?: master.title
                val canonicalAudio = resolvePlayableAudioUrl(id, effectiveLang)
                    ?: (master.audioFileUrl?.takeIf { it.isNotBlank() }
                        ?.let { if (it.startsWith("stories/")) pathToPlayableUrl(it, id, effectiveLang) else it })
                master.toResponse(
                    coverImageUrlResolver.resolveCoverPath(master.coverImageUrl),
                    coverImageUrlResolver.resolveCoverVideoPath(master.coverVideoUrl)
                ).copy(
                    title = displayTitle,
                    content = displayContent,
                    language = effectiveLang,
                    wordCount = masterTranslation?.wordCount ?: master.wordCount,
                    readingTimeMinutes = masterTranslation?.readingTimeMinutes ?: master.readingTimeMinutes,
                    moral = masterTranslation?.moral?.takeIf { it.isNotBlank() } ?: master.moral,
                    audioFileUrl = canonicalAudio,
                    narratedContent = narratedContent,
                    sourceContent = sourceContent,
                    preferNarratedContentForEditor = preferNarrated
                )
            }
            else -> {
                val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(id, effectiveLang)
                val narratedFromScript = translation?.let {
                    narrationScriptRepository.findByTranslationId(it.id)?.scriptText?.takeIf { it.isNotBlank() }
                }
                val contentToServe = translation?.content?.takeIf { it.isNotBlank() } ?: narratedFromScript
                if (translation != null && !contentToServe.isNullOrBlank()) {
                    log.debug("Serving translation id={} lang={} translationId={}", id, effectiveLang, translation.id)
                    val narratedContent = narratedFromScript
                    val sourceContent = translation.content?.takeIf { it.isNotBlank() } ?: ""
                    val preferNarrated = preferNarratedScriptForAdminEditor(narratedFromScript, translation.status)
                    return LibraryStoryResponse(
                        id = master.id,
                        title = rejectIfTamilWhenNotTa(translation.title, effectiveLang) ?: "",
                        content = contentToServe,
                        theme = master.theme,
                        category = master.category,
                        language = effectiveLang,
                        age = master.age,
                        childName = master.childName,
                        wordCount = translation.wordCount,
                        readingTimeMinutes = translation.readingTimeMinutes,
                        moral = rejectIfTamilWhenNotTa(translation.moral, effectiveLang) ?: "",
                        audioFileUrl = resolvePlayableAudioUrl(id, effectiveLang),
                        status = master.status,
                        coverImageUrl = coverImageUrlResolver.resolveCoverPath(master.coverImageUrl),
                        coverVideoUrl = coverImageUrlResolver.resolveCoverVideoPath(master.coverVideoUrl),
                        createdAt = master.createdAt,
                        modifiedAt = master.updatedAt,
                        storyOwner = master.storyOwner,
                        convertPromptUsed = master.convertPromptUsed,
                        emotionMode = master.emotionMode,
                        narrationApprovedAt = master.narrationApprovedAt,
                        narratedContent = narratedContent,
                        sourceContent = sourceContent,
                        preferNarratedContentForEditor = preferNarrated,
                        regeneratePromptLocked = master.regeneratePromptLocked,
                        regeneratePromptLockApproved = master.regeneratePromptLockApproved,
                        regeneratePromptUnlockRequestedAt = master.regeneratePromptUnlockRequestedAt,
                        parentDiscussionPrompts = master.parentDiscussionPrompts,
                        parentContentNote = master.parentContentNote,
                        speakAlongPrompt = master.speakAlongPrompt,
                    )
                }
                // No row or blank text: still surface script-only rows so admin sees pipeline output
                val narratedContent = narratedFromScript
                val fallbackBody = translation?.content?.takeIf { it.isNotBlank() } ?: narratedFromScript ?: ""
                val sourceContent = translation?.content?.takeIf { it.isNotBlank() } ?: ""
                val preferNarrated = preferNarratedScriptForAdminEditor(narratedFromScript, translation?.status)
                master.toResponse(
                    coverImageUrlResolver.resolveCoverPath(master.coverImageUrl),
                    coverImageUrlResolver.resolveCoverVideoPath(master.coverVideoUrl)
                ).copy(
                    title = rejectIfTamilWhenNotTa(translation?.title, effectiveLang) ?: "",
                    content = fallbackBody,
                    moral = rejectIfTamilWhenNotTa(translation?.moral, effectiveLang) ?: "",
                    wordCount = translation?.wordCount ?: 0,
                    readingTimeMinutes = translation?.readingTimeMinutes ?: 0.0,
                    language = effectiveLang,
                    audioFileUrl = null,
                    narratedContent = narratedContent,
                    sourceContent = sourceContent,
                    preferNarratedContentForEditor = preferNarrated
                )
            }
        }
    }

    fun findAll(page: Int, size: Int): Page<LibraryStory> =
        repository.findAll(PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100)))

    /**
     * Listing with projection (no content). Use for list views.
     */
    fun findListingByLanguage(language: String, page: Int, size: Int): Page<LibraryStoryListingResponse> {
        val effectiveLang = effectiveLanguage(language)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        return when {
            isPrimaryCatalogLanguage(effectiveLang) -> repository.findListingByLanguage(effectiveLang, pageable).map { listing ->
                LibraryStoryListingResponse(
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
                    LibraryStoryListingResponse(
                        id = master.id,
                        title = rejectIfTamilWhenNotTa(t.title, effectiveLang) ?: "",
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
     * Same as findListingByLanguage but only stories approved for delivery (narrationApprovedAt set).
     * Used by the public API so only human-verified stories appear on the app.
     */
    fun findListingByLanguageApprovedOnly(language: String, page: Int, size: Int): Page<LibraryStoryListingResponse> {
        val effectiveLang = effectiveLanguage(language)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        return when {
            isPrimaryCatalogLanguage(effectiveLang) -> repository.findListingByLanguageAndNarrationApproved(effectiveLang, pageable).map { listing ->
                LibraryStoryListingResponse(
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
                val translations =
                    if (appProperties.translationPipeline.masterOnlyNarration) {
                        storyTranslationRepository.findListingByLanguageAndMasterNarrationApprovedWithMasterAudio(
                            effectiveLang,
                            pageable
                        )
                    } else {
                        storyTranslationRepository.findListingByLanguageAndMasterNarrationApproved(effectiveLang, pageable)
                    }
                val masterIds = translations.content.map { it.masterStoryId }.distinct()
                val masters = repository.findListingByIdIn(masterIds).associateBy { it.id }
                val content = translations.content.map { t ->
                    val master = masters[t.masterStoryId]!!
                    LibraryStoryListingResponse(
                        id = master.id,
                        title = rejectIfTamilWhenNotTa(t.title, effectiveLang) ?: "",
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
     * Distinct themes (categories) for approved stories in a language.
     * Primary catalog ([isPrimaryCatalogLanguage]): from master rows. Other locales: translation-based queries.
     * Used by GET /stories/library/categories.
     */
    fun getCategories(language: String): List<String> {
        val effectiveLang = effectiveLanguage(language)
        val base = if (isPrimaryCatalogLanguage(effectiveLang)) {
            repository.findDistinctThemesByNarrationApproved(effectiveLang)
        } else if (appProperties.translationPipeline.masterOnlyNarration) {
            repository.findDistinctThemesByNarrationApprovedAndTranslationLanguageWithMasterAudio(effectiveLang)
        } else {
            repository.findDistinctThemesByNarrationApprovedAndTranslationLanguage(effectiveLang)
        }
        val funLaneFirst = listOf("Fun stories", "Funny Stories")
        val presentFun = funLaneFirst.filter { want -> base.any { it.equals(want, ignoreCase = true) } }
        val rest = base.filter { b -> funLaneFirst.none { it.equals(b, ignoreCase = true) } }
        return presentFun + rest
    }

    /**
     * Same as [findByLanguage] but only stories approved for delivery (`narrationApprovedAt` set)
     * **and** with ready default-voice narration audio for the requested language (or legacy master `audioFileUrl` on primary-catalog masters).
     * Used by the parent app library API so titles in review or awaiting new TTS after a re-edit do not appear until audio exists.
     */
    fun findByLanguageApprovedOnly(
        language: String,
        page: Int,
        size: Int,
        theme: String? = null,
        /** When true, lists stories whose category or theme starts with "Learn" (educational hub). */
        learnHub: Boolean = false,
    ): Page<LibraryStoryResponse> {
        val effectiveLang = effectiveLanguage(language)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val effectiveTheme = theme?.trim()?.takeIf { it.isNotBlank() }
        val learnPrefix = "Learn"

        return when {
            isPrimaryCatalogLanguage(effectiveLang) -> {
                val pageResult = when {
                    learnHub -> repository.findByLanguageAndNarrationApprovedLearnPrefix(effectiveLang, learnPrefix, pageable)
                    effectiveTheme != null -> repository.findByLanguageAndNarrationApprovedAndTheme(effectiveLang, effectiveTheme, pageable)
                    else -> repository.findByLanguageAndNarrationApproved(effectiveLang, pageable)
                }
                pageResult.map {
                    val canonicalAudio = resolvePlayableAudioUrl(it.id, effectiveLang)
                        ?: (it.audioFileUrl?.takeIf { u -> u.isNotBlank() }
                            ?.let { p -> if (p.startsWith("stories/")) pathToPlayableUrl(p, it.id, effectiveLang) else p })
                    it.toResponse(
                        coverImageUrlResolver.resolveCoverPath(it.coverImageUrl),
                        coverImageUrlResolver.resolveCoverVideoPath(it.coverVideoUrl)
                    ).copy(
                        audioFileUrl = canonicalAudio
                    )
                }
            }
            else -> {
                val translations = when {
                    learnHub && appProperties.translationPipeline.masterOnlyNarration ->
                        storyTranslationRepository.findByLanguageAndMasterNarrationApprovedWithMasterAudioAndLearnPrefix(
                            effectiveLang, learnPrefix, pageable
                        )
                    learnHub ->
                        storyTranslationRepository.findByLanguageAndMasterNarrationApprovedAndLearnPrefix(
                            effectiveLang, learnPrefix, pageable
                        )
                    appProperties.translationPipeline.masterOnlyNarration && effectiveTheme != null ->
                        storyTranslationRepository.findByLanguageAndMasterNarrationApprovedWithMasterAudioAndTheme(
                            effectiveLang, effectiveTheme, pageable
                        )
                    appProperties.translationPipeline.masterOnlyNarration ->
                        storyTranslationRepository.findByLanguageAndMasterNarrationApprovedWithMasterAudio(effectiveLang, pageable)
                    effectiveTheme != null ->
                        storyTranslationRepository.findByLanguageAndMasterNarrationApprovedAndTheme(effectiveLang, effectiveTheme, pageable)
                    else ->
                        storyTranslationRepository.findByLanguageAndMasterNarrationApproved(effectiveLang, pageable)
                }
                val content = translations.content.map { t ->
                    val master = repository.findById(t.masterStoryId)!!
                    LibraryStoryResponse(
                        id = master.id,
                        title = rejectIfTamilWhenNotTa(t.title, effectiveLang) ?: "",
                        content = t.content,
                        theme = master.theme,
                        category = master.category,
                        language = effectiveLang,
                        age = master.age,
                        childName = master.childName,
                        wordCount = t.wordCount,
                        readingTimeMinutes = t.readingTimeMinutes,
                        moral = rejectIfTamilWhenNotTa(t.moral, effectiveLang) ?: "",
                        audioFileUrl = resolvePlayableAudioUrl(t.masterStoryId, effectiveLang),
                        status = master.status,
                        coverImageUrl = coverImageUrlResolver.resolveCoverPath(master.coverImageUrl),
                        coverVideoUrl = coverImageUrlResolver.resolveCoverVideoPath(master.coverVideoUrl),
                        createdAt = master.createdAt,
                        modifiedAt = master.updatedAt,
                        storyOwner = master.storyOwner,
                        convertPromptUsed = master.convertPromptUsed,
                        emotionMode = master.emotionMode,
                        narrationApprovedAt = master.narrationApprovedAt,
                        sourceContent = t.content,
                        parentDiscussionPrompts = master.parentDiscussionPrompts,
                        parentContentNote = master.parentContentNote,
                        speakAlongPrompt = master.speakAlongPrompt,
                    )
                }
                PageImpl(content, translations.pageable, translations.totalElements)
            }
        }
    }

    /**
     * Primary catalog language: from `library_stories`. Other languages: `story_translations` joined with master + audio.
     */
    fun findByLanguage(language: String, page: Int, size: Int): Page<LibraryStoryResponse> {
        val effectiveLang = effectiveLanguage(language)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        return when {
            isPrimaryCatalogLanguage(effectiveLang) -> repository.findByLanguage(effectiveLang, pageable).map {
                val canonicalAudio = resolvePlayableAudioUrl(it.id, effectiveLang)
                    ?: (it.audioFileUrl?.takeIf { u -> u.isNotBlank() }
                        ?.let { p -> if (p.startsWith("stories/")) pathToPlayableUrl(p, it.id, effectiveLang) else p })
                it.toResponse(
                    coverImageUrlResolver.resolveCoverPath(it.coverImageUrl),
                    coverImageUrlResolver.resolveCoverVideoPath(it.coverVideoUrl)
                ).copy(
                    audioFileUrl = canonicalAudio
                )
            }
            else -> {
                val translations = storyTranslationRepository.findByLanguage(effectiveLang, pageable)
                val content = translations.content.map { t ->
                    val master = repository.findById(t.masterStoryId)!!
                    LibraryStoryResponse(
                        id = master.id,
                        title = rejectIfTamilWhenNotTa(t.title, effectiveLang) ?: "",
                        content = t.content,
                        theme = master.theme,
                        language = effectiveLang,
                        age = master.age,
                        childName = master.childName,
                        wordCount = t.wordCount,
                        readingTimeMinutes = t.readingTimeMinutes,
                        moral = rejectIfTamilWhenNotTa(t.moral, effectiveLang) ?: "",
                        audioFileUrl = resolvePlayableAudioUrl(t.masterStoryId, effectiveLang),
                        status = master.status,
                        coverImageUrl = coverImageUrlResolver.resolveCoverPath(master.coverImageUrl),
                        coverVideoUrl = coverImageUrlResolver.resolveCoverVideoPath(master.coverVideoUrl),
                        createdAt = master.createdAt,
                        modifiedAt = master.updatedAt,
                        storyOwner = master.storyOwner,
                        convertPromptUsed = master.convertPromptUsed,
                        emotionMode = master.emotionMode,
                        narrationApprovedAt = master.narrationApprovedAt,
                        sourceContent = t.content
                    )
                }
                PageImpl(content, translations.pageable, translations.totalElements)
            }
        }
    }

    /**
     * Stories pending narration review.
     * Includes PUBLISHED (queued), PROCESSING (pipeline running), READY (pipeline done, awaiting approve).
     * Excludes approved (narrationApprovedAt not null), CHANGES_REQUESTED, REJECTED, DRAFT.
     * Used by admin "Story for review" queue.
     */
    fun findPendingNarrationReview(page: Int, size: Int): Page<LibraryStory> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        return repository.findByStatusPublishedAndNarrationApprovedAtNull(pageable)
    }

    /**
     * Approved stories (content approved) for "Story to Speech": trigger TTS pipeline from this queue after approval.
     * Used when audio-after-approval=true so audio is generated only after content approval.
     */
    fun findApprovedForSpeech(page: Int, size: Int): Page<LibraryStory> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        return repository.findByNarrationApprovedAtNotNull(pageable)
    }

    fun findAllByStatus(
        status: String?,
        page: Int,
        size: Int,
        narrationApproved: Boolean? = null
    ): Page<LibraryStory> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        return if (status != null && status.isNotBlank()) {
            val s = status.trim().uppercase()
            if (s == "PUBLISHED") {
                val statuses = listOf("PUBLISHED", "PROCESSING", "READY")
                when (narrationApproved) {
                    true -> repository.findByStatusInWithProcessingFirstAndNarrationApprovedAtNotNull(statuses, pageable)
                    false -> repository.findByStatusInWithProcessingFirstAndNarrationApprovedAtNull(statuses, pageable)
                    null -> repository.findByStatusInWithProcessingFirst(statuses, pageable)
                }
            } else {
                when (narrationApproved) {
                    true -> repository.findByStatusAndNarrationApprovedAtNotNull(s, pageable)
                    false -> repository.findByStatusAndNarrationApprovedAtNull(s, pageable)
                    null -> repository.findByStatus(s, pageable)
                }
            }
        } else {
            when (narrationApproved) {
                true -> repository.findAllWithProcessingFirstAndNarrationApprovedAtNotNull(pageable)
                false -> repository.findAllWithProcessingFirstAndNarrationApprovedAtNull(pageable)
                null -> repository.findAllWithProcessingFirst(pageable)
            }
        }
    }

    fun bulkPublish(ids: List<Long>): Int =
        repository.updateStatusBulk(ids, StoryStatus.PUBLISHED)

    private val reviewQueueStatuses = StoryStatus.REVIEW_QUEUE

    /**
     * Request changes: send story back to author. Valid when in review queue (PUBLISHED, PROCESSING, READY) and narrationApprovedAt=null.
     */
    @Transactional
    fun requestChanges(id: Long, notes: String?): Boolean {
        val story = repository.findById(id) ?: return false
        if (story.status !in reviewQueueStatuses || story.narrationApprovedAt != null) return false
        repository.updateStatusAndReviewNotes(id, StoryStatus.CHANGES_REQUESTED, notes?.take(2000))
        // Reset review-cycle markers so a future submit/review starts fresh.
        repository.updateRejectMarkedAt(id, null)
        libraryStoryLanguageReviewJpaRepository.deleteByLibraryStoryId(id)
        resetPipelineStateAfterReviewTerminalDecision(id)
        log.info("Story id={} sent back for changes (pipeline statuses reset)", id)
        return true
    }

    /**
     * Reject story in review flow: sets status to REJECTED only (row stays in DB; no delete).
     * Author opens Story library → Edit, updates content, then Submit for review (PUBLISHED) to re-queue.
     * Valid when in review queue (PUBLISHED, PROCESSING, READY) and narrationApprovedAt=null.
     */
    @Transactional
    fun reject(id: Long, notes: String?): Boolean {
        val story = repository.findById(id) ?: return false
        if (story.status !in reviewQueueStatuses || story.narrationApprovedAt != null) return false
        repository.updateStatusAndReviewNotes(id, StoryStatus.REJECTED, notes?.take(2000))
        repository.updateRejectMarkedAt(id, null)
        libraryStoryLanguageReviewJpaRepository.deleteByLibraryStoryId(id)
        resetPipelineStateAfterReviewTerminalDecision(id)
        log.info("Story id={} rejected (pipeline statuses reset)", id)
        return true
    }

    /** Set or clear reject-marked flag (admin ticked "Reject this story" in language view). */
    @Transactional
    fun setRejectMarked(id: Long, marked: Boolean): Boolean {
        val story = repository.findById(id) ?: return false
        if (story.status !in reviewQueueStatuses || story.narrationApprovedAt != null) return false
        repository.updateRejectMarkedAt(id, if (marked) java.time.Instant.now() else null)
        log.info("Story id={} reject-marked={}", id, marked)
        return true
    }

    /**
     * Mark narration as approved for final delivery (human verification).
     * Sets narration_approved_at and status=PUBLISHED so the story appears on the app.
     * When [AppProperties.translationPipeline.autoTtsOnApprove] and [audioAfterApproval] are true and audio is still
     * needed, starts background TTS. Default is autoTtsOnApprove=false: use Narration → Generate audio after approval.
     * Valid only when story is in review queue (PUBLISHED, PROCESSING, READY) and not yet approved.
     */
    @Transactional
    fun approveNarration(id: Long): Boolean {
        val story = repository.findById(id) ?: return false
        if (story.status !in reviewQueueStatuses || story.narrationApprovedAt != null) return false
        if (!allPipelineLanguagesReviewed(id, story)) {
            log.warn("Narration approval blocked for story id={} because not all languages are reviewed", id)
            return false
        }
        repository.updateNarrationApprovedAt(id, java.time.Instant.now())
        repository.updateStatus(id, StoryStatus.PUBLISHED)
        repository.updateRejectMarkedAt(id, null)
        log.info("Narration approved for story id={}; story is now published and visible on the app", id)
        if (appProperties.translationPipeline.autoTtsOnApprove &&
            appProperties.translationPipeline.audioAfterApproval &&
            needsPostApprovalNarrationAudio(id)
        ) {
            triggerPipelineExecutor.execute {
                try {
                    storyProcessingService.regenerateNarration(id, null)
                    log.info("Post-approval TTS finished for story id={}", id)
                } catch (e: Exception) {
                    log.error("Post-approval TTS failed for story id={}: {}", id, e.message, e)
                }
            }
        } else if (appProperties.translationPipeline.audioAfterApproval && appProperties.translationPipeline.autoTtsOnApprove) {
            log.info("Post-approval TTS skipped for story id={} (narration audio already ready for all languages)", id)
        } else if (appProperties.translationPipeline.audioAfterApproval && !appProperties.translationPipeline.autoTtsOnApprove) {
            log.info("Post-approval TTS not auto-started for story id={} (auto-tts-on-approve=false; use Narration → Generate audio)", id)
        }
        return true
    }

    /**
     * True when any pipeline language still needs synthesized audio after content approval
     * (e.g. [TranslationPipelineStatus.AWAITING_AUDIO], missing narration row, or not READY).
     * When false for all languages, [approveNarration] does not call regenerate (avoids wiping good audio from legacy flows).
     */
    private fun needsPostApprovalNarrationAudio(masterStoryId: Long): Boolean {
        val story = repository.findById(masterStoryId) ?: return false
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
        val supportedLangs = (listOf(sourceLang) + appProperties.translationPipeline.targetLanguages
            .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }).distinct()
        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        if (appProperties.translationPipeline.masterOnlyNarration) {
            val masterLang = story.language.trim().lowercase().take(10).ifEmpty { sourceLang }
            val translation = translations.find { it.language.equals(masterLang, ignoreCase = true) } ?: return true
            if (translation.status == TranslationPipelineStatus.AWAITING_AUDIO) return true
            if (translation.status.isFailed()) return true
            if (!story.audioFileUrl.isNullOrBlank()) return false
            val hasReady = narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
                translation.id, "default", NarrationAudioStatus.READY
            )
            return !hasReady
        }
        if (translations.any { it.status == TranslationPipelineStatus.AWAITING_AUDIO }) return true
        for (lang in supportedLangs) {
            val translation = translations.find { it.language.equals(lang, ignoreCase = true) } ?: return true
            if (translation.status.isFailed()) return true
            if (lang == sourceLang && !story.audioFileUrl.isNullOrBlank()) continue
            val hasReady = narrationAudioRepository.existsByTranslationIdAndVoiceProfileAndStatus(
                translation.id, "default", NarrationAudioStatus.READY
            )
            if (!hasReady) return true
        }
        return false
    }

    private fun allPipelineLanguagesReviewed(masterStoryId: Long, story: LibraryStory): Boolean {
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
        val supportedLangs = (listOf(sourceLang) + appProperties.translationPipeline.targetLanguages
            .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }).distinct()
        if (supportedLangs.isEmpty()) return false
        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        val reviewRows = libraryStoryLanguageReviewJpaRepository.findByLibraryStoryId(masterStoryId)
        val reviewedLangs = reviewRows.map { it.language.trim().lowercase() }.toSet()
        val staleReviewed = reviewRows.mapNotNull { row ->
            val lang = row.language.trim().lowercase()
            val stored = row.contentFingerprint?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val current = fingerprintLanguageContent(story, lang, sourceLang, translations)
            if (current != stored) lang else null
        }.toSet()
        val missingLangs = supportedLangs.filter { it !in reviewedLangs }
        return missingLangs.isEmpty() && staleReviewed.isEmpty()
    }

    /**
     * Approve narration for a single language (per-language human verification).
     */
    @Transactional
    fun approveTranslationNarration(masterStoryId: Long, language: String): Boolean {
        val effectiveLang = effectiveLanguage(language)
        val existing = storyTranslationRepository.findByMasterStoryIdAndLanguage(masterStoryId, effectiveLang)
            ?: return false
        storyTranslationRepository.updateNarrationApprovedAt(masterStoryId, effectiveLang, java.time.Instant.now())
        log.info("Translation narration approved masterStoryId={} language={}", masterStoryId, effectiveLang)
        return true
    }

    /** Returns which languages have narration approved for a story (for admin list). */
    fun getTranslationNarrationApproval(masterStoryId: Long): Map<String, Boolean> =
        storyTranslationRepository.getNarrationApprovalByMasterStoryId(masterStoryId)

    /**
     * Applies admin-provided narration script text for the story's current language (translation row).
     * [patch] null = omit from JSON, leave script unchanged; empty = delete script; non-empty = upsert.
     */
    private fun applyNarratedContentPatch(masterStoryId: Long, language: String, patch: String?) {
        if (patch == null) return
        val effectiveLang = effectiveLanguage(language)
        val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(masterStoryId, effectiveLang)
            ?: run {
                log.warn("Skipping narrated content patch: no translation row for storyId={} lang={}", masterStoryId, effectiveLang)
                return
            }
        val trimmed = patch.trim()
        val existing = narrationScriptRepository.findByTranslationId(translation.id)
        if (trimmed.isEmpty()) {
            if (existing != null) {
                narrationScriptRepository.deleteByTranslationId(translation.id)
                log.info("Removed narration script after empty admin patch translationId={}", translation.id)
            }
            return
        }
        val limited = trimmed.take(50_000)
        if (existing != null) {
            if (normalizeForCompare(existing.scriptText) == normalizeForCompare(limited)) return
            narrationScriptRepository.save(existing.copy(scriptText = limited))
            log.info("Updated narration script from admin patch translationId={}", translation.id)
        } else {
            narrationScriptRepository.save(
                StoryNarrationScript(
                    id = 0L,
                    translationId = translation.id,
                    toneMode = ToneMode.CALM,
                    scriptText = limited,
                    safetyScore = 100,
                    createdAt = java.time.Instant.now()
                )
            )
            log.info("Created narration script from admin patch translationId={}", translation.id)
        }
    }

    /**
     * Update a story translation's title, content, and/or moral (per-language edit).
     * Recomputes wordCount and readingTimeMinutes when content is provided.
     * If no story_translation row exists yet for this language, creates one (from master for source language,
     * or from request body for other languages) so admin edits from Story for review succeed for all languages.
     */
    @Transactional
    fun updateTranslationContent(
        masterStoryId: Long,
        language: String,
        title: String?,
        content: String?,
        moral: String?
    ): Boolean {
        val effectiveLang = effectiveLanguage(language)
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
        val existing = storyTranslationRepository.findByMasterStoryIdAndLanguage(masterStoryId, effectiveLang)

        if (existing == null) {
            val master = repository.findById(masterStoryId) ?: return false
            val (newTitle, newContent, newMoral) = if (effectiveLang == sourceLang) {
                // Source language: use master content as default
                Triple(
                    title?.takeIf { it.isNotBlank() } ?: master.title,
                    content?.takeIf { it.isNotBlank() } ?: master.content,
                    moral?.takeIf { it.isNotBlank() } ?: master.moral
                )
            } else {
                // Other languages: require request body (admin is providing/editing the translation)
                val t = title?.takeIf { it.isNotBlank() }
                val c = content?.takeIf { it.isNotBlank() }
                val m = moral?.takeIf { it.isNotBlank() }
                if (c.isNullOrBlank()) return false
                Triple(t ?: "", c, m)
            }
            val wordCount = newContent.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val readingTimeMinutes = (wordCount / 150.0).coerceAtMost(5.0)
            if (effectiveLang == sourceLang) {
                repository.update(
                    master.copy(
                        title = newTitle,
                        content = newContent,
                        moral = newMoral?.take(2000),
                        wordCount = wordCount,
                        readingTimeMinutes = readingTimeMinutes,
                        updatedAt = java.time.Instant.now()
                    )
                )
            }
            val newTranslation = StoryTranslation(
                id = 0,
                masterStoryId = masterStoryId,
                language = effectiveLang,
                title = newTitle,
                content = newContent,
                moral = newMoral?.take(2000),
                wordCount = wordCount,
                readingTimeMinutes = readingTimeMinutes,
                createdAt = java.time.Instant.now(),
                status = TranslationPipelineStatus.PENDING,
                retryCount = 0,
                lastError = null,
                narrationApprovedAt = null
            )
            storyTranslationRepository.save(newTranslation)
            log.info("Translation created for language masterStoryId={} language={}", masterStoryId, effectiveLang)
            return true
        }

        val newTitle = title?.takeIf { it.isNotBlank() } ?: existing.title
        val newContent = content?.takeIf { it.isNotBlank() } ?: existing.content
        val newMoral = moral?.takeIf { it.isNotBlank() } ?: existing.moral
        val translationChanged = normalizeForCompare(newTitle) != normalizeForCompare(existing.title) ||
            normalizeForCompare(newContent) != normalizeForCompare(existing.content) ||
            normalizeForCompare(newMoral ?: "") != normalizeForCompare(existing.moral ?: "")
        if (!translationChanged) {
            log.debug(
                "Translation unchanged; skipping save masterStoryId={} language={}",
                masterStoryId,
                effectiveLang
            )
            return true
        }
        val wordCount = newContent.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val readingTimeMinutes = (wordCount / 150.0).coerceAtMost(5.0)
        val updated = StoryTranslation(
            id = existing.id,
            masterStoryId = existing.masterStoryId,
            language = existing.language,
            title = newTitle,
            content = newContent,
            moral = newMoral?.take(2000),
            wordCount = wordCount,
            readingTimeMinutes = readingTimeMinutes,
            createdAt = existing.createdAt,
            status = if (translationChanged) TranslationPipelineStatus.PENDING else existing.status,
            retryCount = if (translationChanged) 0 else existing.retryCount,
            lastError = if (translationChanged) null else existing.lastError,
            narrationApprovedAt = if (translationChanged) null else existing.narrationApprovedAt
        )
        storyTranslationRepository.save(updated)
        narrationScriptRepository.deleteByTranslationId(existing.id)
        repository.updateNarrationApprovedAt(masterStoryId, null)
        storyLibrarySelf.getObject().invalidateNarrationAudioForLanguages(masterStoryId, listOf(effectiveLang))
        log.info(
            "Translation changed; reset approval/audio state masterStoryId={} language={} and moved translation to PENDING",
            masterStoryId,
            effectiveLang
        )
        log.info("Translation updated masterStoryId={} language={}", masterStoryId, effectiveLang)
        return true
    }

    fun requestRegeneratePromptUnlock(masterStoryId: Long): Boolean {
        if (repository.findById(masterStoryId) == null) return false
        repository.updateRegeneratePromptUnlockRequestedAt(masterStoryId, java.time.Instant.now())
        log.info("Regenerate-with-prompt unlock requested masterStoryId={}", masterStoryId)
        return true
    }

    fun approveRegeneratePromptUnlock(masterStoryId: Long): Boolean {
        if (repository.findById(masterStoryId) == null) return false
        repository.approveRegeneratePromptUnlock(masterStoryId)
        log.info("Regenerate-with-prompt unlock approved masterStoryId={}", masterStoryId)
        return true
    }

    fun bulkUpdateCategory(ids: List<Long>, theme: String): Int =
        repository.updateThemeBulk(ids, theme.trim().take(100))

    /**
     * Clear library story audio_file_url so only story_narration_audio is used for serving.
     * Legacy story_audio table removed; this only clears library_stories.audio_file_url.
     */
    @Transactional
    fun clearLegacyAudio(): Triple<Int, Int, Int> {
        val clearedUrls = repository.clearAllAudioUrls()
        log.info("Legacy audio URLs cleared: library stories audio_file_url cleared={}", clearedUrls)
        return Triple(0, clearedUrls, 0)
    }

    /**
     * Clear narration audio (and S3 objects) for the given languages before republish/regenerate.
     * Deletes story_narration_audio rows, S3 objects, and clears audio_file_url when source lang is included.
     * Uses REQUIRES_NEW so changes commit immediately; pipeline-status API will see PENDING before pipeline runs.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun invalidateNarrationAudioForLanguages(masterStoryId: Long, languages: List<String>): Int {
        if (languages.isEmpty()) return 0
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
        val masterLang = repository.findById(masterStoryId)?.language?.trim()?.lowercase()?.take(10)
        var totalDeleted = 0
        val allKeys = mutableSetOf<String>()
        val s3Enabled = s3Client != null && appProperties.storage.type == "s3"
        for (lang in languages.distinct()) {
            val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(masterStoryId, lang) ?: continue
            val narrationRows = narrationAudioRepository.findAllByTranslationId(translation.id)
            if (s3Enabled) {
                for (row in narrationRows) {
                    val u = row.audioUrl ?: continue
                    val k = when {
                        u.startsWith("stories/") -> u
                        "/stories/" in u -> u.substringAfter("/stories/").let { "stories/$it" }
                        else -> null
                    }
                    if (k != null) allKeys.add(k)
                }
            }
            narrationAudioRepository.deleteByTranslationId(translation.id)
            storyTranslationRepository.atomicStatusUpdate(translation.id, TranslationPipelineStatus.PENDING, null)
            totalDeleted += narrationRows.size
        }
        // Blocking delete avoids a race where async deletion can remove freshly regenerated files
        // that reuse the same S3 keys (e.g. stories/{id}/{lang}/v1.mp3).
        deleteS3KeysBlocking(allKeys, "invalidate-narration masterStoryId=$masterStoryId")
        val clearsMasterLegacyAudio = if (appProperties.translationPipeline.masterOnlyNarration) {
            (masterLang != null && languages.any { it.equals(masterLang, ignoreCase = true) }) ||
                languages.any { it.equals(sourceLang, ignoreCase = true) }
        } else {
            languages.any { it.equals(sourceLang, ignoreCase = true) }
        }
        if (clearsMasterLegacyAudio) {
            repository.clearAudioUrl(masterStoryId)
        }
        progressTracker.clearProcessing(masterStoryId)
        ttsMetadataCache?.invalidateForStory(masterStoryId, languages)
        log.info("Invalidated narration audio for story id={} languages={} (deleted {} rows, S3 objects removed)", masterStoryId, languages, totalDeleted)
        return totalDeleted
    }

    /**
     * Clear stale narration audio for one language when the file is missing in S3 (e.g. after NoSuchKey on preview).
     * Deletes the default-voice narration row and sets translation status to PENDING so pipeline status and next
     * preview request reflect that audio needs to be regenerated.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun clearStaleNarrationAudioForLanguage(masterStoryId: Long, language: String): Boolean {
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
        val effectiveLang = language.trim().lowercase().take(10)
        val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(masterStoryId, effectiveLang) ?: return false
        narrationAudioRepository.deleteByTranslationIdAndVoiceProfile(translation.id, "default")
        storyTranslationRepository.atomicStatusUpdate(translation.id, TranslationPipelineStatus.PENDING, "Audio missing in S3; cleared for regeneration")
        if (effectiveLang == sourceLang) {
            repository.clearAudioUrl(masterStoryId)
        }
        progressTracker.clearProcessing(masterStoryId)
        log.info("Cleared stale narration audio for story id={} lang={} (S3 file missing)", masterStoryId, effectiveLang)
        return true
    }

    /**
     * Clear stale narration by S3 key when the file is missing in storage. Finds the narration row by matching
     * audioUrl to the key (so language param mismatch with DB does not prevent clearing). Use when NoSuchKey
     * occurs so pipeline status stops showing "Done" for that story.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun clearStaleNarrationByStorageKey(masterStoryId: Long, storageKey: String): Boolean {
        if (!storageKey.startsWith("stories/")) return false
        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        if (translations.isEmpty()) return false
        val translationIds = translations.map { it.id }
        val narrations = narrationAudioRepository.findByTranslationIdInAndVoiceProfile(translationIds, "default")
        val match = narrations.find { it.audioUrl?.trim() == storageKey.trim() } ?: return false
        val translation = translations.find { it.id == match.translationId } ?: return false
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
        narrationAudioRepository.deleteByTranslationIdAndVoiceProfile(translation.id, "default")
        storyTranslationRepository.atomicStatusUpdate(translation.id, TranslationPipelineStatus.PENDING, "Audio missing in S3; cleared for regeneration")
        if (translation.language.trim().lowercase() == sourceLang) {
            repository.clearAudioUrl(masterStoryId)
        }
        progressTracker.clearProcessing(masterStoryId)
        log.info("Cleared stale narration by key for story id={} translationId={} key={}", masterStoryId, translation.id, storageKey)
        return true
    }

    /**
     * Reset narration status for a story so it shows as Pending and admin can run "Regenerate audio".
     * Clears all narration rows and translation status for this story (DB only; does not delete from S3).
     * Use when audio is missing in S3 but status still shows "Done" — then run Regenerate audio.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun resetNarrationStatusForStory(masterStoryId: Long): Int {
        if (repository.findById(masterStoryId) == null) return 0
        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        var cleared = 0
        for (t in translations) {
            narrationAudioRepository.deleteByTranslationId(t.id)
            storyTranslationRepository.atomicStatusUpdate(t.id, TranslationPipelineStatus.PENDING, "Reset for regeneration")
            cleared++
        }
        repository.clearAudioUrl(masterStoryId)
        progressTracker.clearProcessing(masterStoryId)
        log.info("Reset narration status for story id={} ({} translation(s)); ready for Regenerate audio", masterStoryId, cleared)
        return cleared
    }

    /** Reset stuck translations (REWRITING, TRANSLATING, TTS_PROCESSING) to PENDING for a story. Returns count reset. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun resetStuckTranslationsToPending(masterStoryId: Long): Int {
        val stuck = listOf(
            TranslationPipelineStatus.REWRITING,
            TranslationPipelineStatus.TRANSLATING,
            TranslationPipelineStatus.TTS_PROCESSING
        )
        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
            .filter { it.status in stuck }
        translations.forEach {
            storyTranslationRepository.atomicStatusUpdate(it.id, TranslationPipelineStatus.PENDING, null)
        }
        progressTracker.clearProcessing(masterStoryId)
        log.info("Reset {} stuck translation(s) to PENDING for masterStoryId={}", translations.size, masterStoryId)
        return translations.size
    }

    /**
     * Full recovery for one story: clear narration rows, reset translation status to PENDING,
     * clear retry counters/errors, and clear master audio URL so pipeline can rerun cleanly.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun resetNarrationAndRetryStateForStory(masterStoryId: Long): Int {
        if (repository.findById(masterStoryId) == null) return 0
        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        translations.forEach { t ->
            narrationAudioRepository.deleteByTranslationId(t.id)
            storyTranslationRepository.atomicStatusUpdate(t.id, TranslationPipelineStatus.PENDING, null)
        }
        storyTranslationRepository.resetRetryCountByMasterStoryId(masterStoryId)
        repository.clearAudioUrl(masterStoryId)
        progressTracker.clearProcessing(masterStoryId)
        log.info(
            "Recovered story for full rerun id={} translations={} (status=PENDING, retry=0, errors cleared, audio cleared)",
            masterStoryId,
            translations.size
        )
        return translations.size
    }

    /**
     * Nuclear cleanup: delete ALL narration audio, S3 objects, and clear audio_file_url.
     * Use when cache/audio is corrupted or stale. Pipeline will regenerate on next approval or trigger.
     */
    @Transactional
    fun clearAllNarrationAndAudio(): Map<String, Int> {
        val narrationRows = narrationAudioRepository.findAll()
        val narrationS3Keys = narrationRows.mapNotNull { it.audioUrl?.takeIf { u -> u.startsWith("stories/") } }.toSet()
        var s3NarrationDeleted = 0
        if (s3Client != null && narrationS3Keys.isNotEmpty() && appProperties.storage.type == "s3") {
            val bucket = appProperties.storage.effectiveS3Bucket
            for (key in narrationS3Keys) {
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
                    s3NarrationDeleted++
                } catch (e: Exception) {
                    log.warn("S3 delete failed for narration key={}: {}", key, e.message)
                }
            }
        }
        narrationAudioRepository.deleteAll()
        val (deletedLegacy, clearedUrls, s3LegacyDeleted) = clearLegacyAudio()
        log.info("Clear-all complete: narration_audio deleted={}, S3 narration={}, legacy_audio={}, urls_cleared={}, S3 legacy={}",
            narrationRows.size, s3NarrationDeleted, deletedLegacy, clearedUrls, s3LegacyDeleted)
        return mapOf(
            "narrationAudioDeleted" to narrationRows.size,
            "s3NarrationDeleted" to s3NarrationDeleted,
            "storyAudioDeleted" to deletedLegacy,
            "audioUrlsCleared" to clearedUrls,
            "s3LegacyDeleted" to s3LegacyDeleted
        )
    }

    /**
     * Soft-delete: hides story from app and admin lists; row kept for [AppProperties.libraryStorySoftDelete.retentionDays]
     * (default 30) then removed by [permanentlyPurgeExpiredSoftDeletes]. Cleans favorites/analytics/playback/feedback now.
     */
    @Transactional
    fun deleteById(id: Long): Boolean {
        if (repository.findById(id) == null) return false
        cleanupRelatedData(id)
        progressTracker.clearProcessing(id)
        val now = java.time.Instant.now()
        if (!repository.markSoftDeleted(id, now)) return false
        log.info(
            "Library story id={} soft-deleted at {} (permanent purge after {} days unless restored)",
            id,
            now,
            appProperties.libraryStorySoftDelete.retentionDays
        )
        return true
    }

    /**
     * Bulk soft-delete. Returns count of stories moved to retention.
     */
    @Transactional
    fun deleteByIds(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        val existing = ids.filter { repository.findById(it) != null }
        val now = java.time.Instant.now()
        var n = 0
        for (storyId in existing) {
            cleanupRelatedData(storyId)
            progressTracker.clearProcessing(storyId)
            if (repository.markSoftDeleted(storyId, now)) n++
        }
        log.info("Library stories soft-deleted count={} ids={}", n, existing)
        return n
    }

    fun findSoftDeleted(page: Int, size: Int): Page<LibraryStory> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        return repository.findSoftDeleted(pageable)
    }

    @Transactional
    fun restoreSoftDeletedLibraryStory(id: Long): Boolean = repository.restoreSoftDeleted(id)

    /**
     * Permanently removes rows soft-deleted longer than retention. Intended for scheduled job.
     * Not transactional as a whole so one failure does not roll back prior deletes.
     */
    fun permanentlyPurgeExpiredSoftDeletes(): Int {
        val days = appProperties.libraryStorySoftDelete.retentionDays.coerceIn(1, 3650)
        val cutoff = java.time.Instant.now().minusSeconds(days * 86400)
        val ids = repository.findIdsSoftDeletedBefore(cutoff)
        var n = 0
        for (id in ids) {
            try {
                repository.hardDeleteById(id)
                n++
                log.info("Library story id={} permanently purged (soft-deleted before {})", id, cutoff)
            } catch (e: Exception) {
                log.error("Permanent purge failed library story id={}", id, e)
            }
        }
        return n
    }

    private fun cleanupRelatedData(storyId: Long) {
        // Do NOT delete family voice recordings: they are the parent's voice profile used for any story.
        favoriteStoryJpaRepository.deleteByStoryIdAndStorySource(storyId, "library")
        storyAnalyticsJpaRepository.deleteByStoryIdAndStorySource(storyId, "library")
        storyPlaybackPositionJpaRepository.deleteByStoryIdAndStorySource(storyId, "library")
        storyFeedbackJpaRepository.deleteByStoryIdAndStorySource(storyId, "library")
    }

    /** Best-effort S3 cleanup off the request thread so admin PUT does not hit multi‑minute client timeouts. */
    private fun deleteS3KeysAsync(keys: Collection<String>, context: String) {
        val client = s3Client
        if (keys.isEmpty() || client == null || appProperties.storage.type != "s3") return
        val bucket = appProperties.storage.effectiveS3Bucket
        val deduped = keys.filter { it.isNotBlank() }.toSet()
        if (deduped.isEmpty()) return
        triggerPipelineExecutor.execute {
            for (key in deduped) {
                try {
                    client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
                } catch (e: Exception) {
                    log.warn("Async S3 delete failed context={} key={}: {}", context, key, e.message)
                }
            }
            log.info("Async S3 delete finished context={} keyCount={}", context, deduped.size)
        }
    }

    /** Synchronous/best-effort delete used before immediate regeneration to avoid stale-key races. */
    private fun deleteS3KeysBlocking(keys: Collection<String>, context: String) {
        val client = s3Client
        if (keys.isEmpty() || client == null || appProperties.storage.type != "s3") return
        val bucket = appProperties.storage.effectiveS3Bucket
        val deduped = keys.filter { it.isNotBlank() }.toSet()
        if (deduped.isEmpty()) return
        for (key in deduped) {
            try {
                client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
            } catch (e: Exception) {
                log.warn("S3 delete failed context={} key={}: {}", context, key, e.message)
            }
        }
        log.info("S3 delete finished context={} keyCount={}", context, deduped.size)
    }

    /**
     * Removes `library_stories.audio_file_url` and, when it points at our bucket (`stories/...`), deletes that object from S3.
     */
    private fun deleteLegacyMasterAudioAndClearUrl(masterStoryId: Long) {
        val story = repository.findById(masterStoryId) ?: return
        val raw = story.audioFileUrl?.trim()?.takeIf { it.isNotBlank() } ?: run {
            repository.clearAudioUrl(masterStoryId)
            return
        }
        val key = when {
            raw.startsWith("stories/") -> raw
            "/stories/" in raw -> raw.substringAfter("/stories/").let { "stories/$it" }
            else -> null
        }
        repository.clearAudioUrl(masterStoryId)
        if (key != null) {
            deleteS3KeysAsync(listOf(key), "legacy-master-audio masterStoryId=$masterStoryId")
        }
    }

    /**
     * After admin **rejects** or **requests changes**: clear in-memory pipeline claim, remove narration rows
     * (and S3 objects when configured), delete legacy master audio (S3 + DB), and set every translation to `PENDING`
     * with no last error, zero retries, and no per-language narration approval.
     *
     * Preserves translation **text** so localized copy is not lost; only pipeline / audio state is reset so
     * the admin "Pipeline" column and the next submit/review cycle start from a consistent baseline.
     */
    private fun resetPipelineStateAfterReviewTerminalDecision(masterStoryId: Long) {
        progressTracker.clearProcessing(masterStoryId)
        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        if (translations.isEmpty()) {
            deleteLegacyMasterAudioAndClearUrl(masterStoryId)
            log.debug("Review terminal pipeline reset: masterStoryId={} has no translations", masterStoryId)
            return
        }
        val terminalResetKeys = mutableSetOf<String>()
        val s3Enabled = s3Client != null && appProperties.storage.type == "s3"
        for (t in translations) {
            val narrationRows = narrationAudioRepository.findAllByTranslationId(t.id)
            if (s3Enabled) {
                for (row in narrationRows) {
                    val u = row.audioUrl ?: continue
                    val k = when {
                        u.startsWith("stories/") -> u
                        "/stories/" in u -> u.substringAfter("/stories/").let { "stories/$it" }
                        else -> null
                    }
                    if (k != null) terminalResetKeys.add(k)
                }
            }
            narrationAudioRepository.deleteByTranslationId(t.id)
        }
        deleteS3KeysAsync(terminalResetKeys, "review-terminal-reset masterStoryId=$masterStoryId")
        val bulkUpdated = storyTranslationRepository.resetPipelineStateForMasterStory(masterStoryId)
        deleteLegacyMasterAudioAndClearUrl(masterStoryId)
        ttsMetadataCache?.invalidateForStory(masterStoryId, translations.map { it.language })
        log.info(
            "Review terminal decision: pipeline reset for masterStoryId={} ({} translation(s), {} row(s) set to PENDING)",
            masterStoryId,
            translations.size,
            bulkUpdated
        )
    }

    /** Clears per-language narration rows + S3 (via [invalidateNarrationAudioForLanguages]) or legacy master audio only. */
    private fun clearAllNarrationAudioForReviewResubmit(masterStoryId: Long) {
        val langs = storyTranslationRepository.findByMasterStoryId(masterStoryId)
            .map { it.language.trim().lowercase() }
            .distinct()
        if (langs.isNotEmpty()) {
            // Must go through Spring proxy so REQUIRES_NEW on invalidateNarrationAudioForLanguages is honored.
            storyLibrarySelf.getObject().invalidateNarrationAudioForLanguages(masterStoryId, langs)
        } else {
            deleteLegacyMasterAudioAndClearUrl(masterStoryId)
        }
    }

    /** Deletes narration audio DB rows; S3 objects are removed asynchronously (see [deleteS3KeysAsync]). */
    private fun deleteNarrationAudioRowsAndS3ForTranslations(translations: List<StoryTranslation>) {
        if (translations.isEmpty()) return
        val s3Enabled = s3Client != null && appProperties.storage.type == "s3"
        val allKeys = mutableSetOf<String>()
        for (t in translations) {
            val narrationRows = narrationAudioRepository.findAllByTranslationId(t.id)
            if (s3Enabled) {
                for (row in narrationRows) {
                    val u = row.audioUrl ?: continue
                    val k = when {
                        u.startsWith("stories/") -> u
                        "/stories/" in u -> u.substringAfter("/stories/").let { "stories/$it" }
                        else -> null
                    }
                    if (k != null) allKeys.add(k)
                }
            }
            narrationAudioRepository.deleteByTranslationId(t.id)
        }
        deleteS3KeysAsync(allKeys, "submit-for-review-reset-narration")
    }

    /**
     * Reset pipeline state when story is submitted for review (first, second, or subsequent time).
     * Clears narration audio (including S3), resets translation pipeline state to PENDING, clears legacy master audio,
     * replaced cover assets, and TTS cache hints. Per-language "reviewed" flags are cleared in [update] before this runs.
     *
     * NOTE: do not hard-delete `story_translations` here. The update flow persists per-language edits in
     * [applyAdminTranslationEditsInNewTransaction] (REQUIRES_NEW). Deleting translation rows in this outer transaction
     * can hold row/index locks long enough to block that inner transaction and make submit-for-review appear hung.
     * Does not change library story status (stays PUBLISHED for review queue).
     */
    private fun resetPipelineStateForSubmitForReview(
        masterStoryId: Long,
        storyBeforeSave: LibraryStory,
        storyAfterSave: LibraryStory
    ) {
        progressTracker.clearProcessing(masterStoryId)
        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        deleteNarrationAudioRowsAndS3ForTranslations(translations)
        ttsMetadataCache?.invalidateForStory(masterStoryId, translations.map { it.language })
        val resetCount = storyTranslationRepository.resetPipelineStateForMasterStory(masterStoryId)
        deleteLegacyMasterAudioAndClearUrl(masterStoryId)
        libraryStoryIllustrationService.deleteReplacedCoverAssets(storyBeforeSave, storyAfterSave)
        log.info(
            "Pipeline state reset for story id={} (submit for review); translation rows set to PENDING={} and status will show PENDING until approval",
            masterStoryId,
            resetCount
        )
    }

    /**
     * Persists admin translation rows + optional narration script patch in a **new** transaction that commits
     * before the outer [update] writes `library_stories`. Otherwise one long transaction holds `story_translations`
     * row locks until the master row is updated, which can block the narration pipeline for minutes and trip the
     * admin client timeout when saving a draft before “Generate translations”.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun applyAdminTranslationEditsInNewTransaction(
        id: Long,
        effectiveLang: String,
        title: String?,
        content: String,
        moral: String?,
        toApply: Map<String, TranslationContentEntryDto>,
        allowedLangs: List<String>,
        narratedContentPatch: String?
    ) {
        updateTranslationContent(id, effectiveLang, title, content, moral)
        if (toApply.isNotEmpty()) {
            for ((lang, entry) in toApply) {
                val toApplyLang = lang.trim().lowercase().take(10)
                if (toApplyLang.isBlank() || toApplyLang !in allowedLangs || toApplyLang == effectiveLang) continue
                if (entry.content.isBlank() && entry.title.isNullOrBlank() && entry.moral.isNullOrBlank()) continue
                val contentToPersist = entry.content.takeIf { it.isNotBlank() }
                updateTranslationContent(id, toApplyLang, entry.title, contentToPersist, entry.moral)
                log.info("Updated translation content for story id={} lang={}", id, toApplyLang)
            }
        }
        applyNarratedContentPatch(id, effectiveLang, narratedContentPatch)
    }

    @Transactional
    fun update(
        id: Long,
        title: String?,
        content: String,
        theme: String,
        category: String? = null,
        language: String,
        age: Int,
        childName: String,
        moral: String?,
        status: String,
        coverImageUrl: String?,
        coverVideoUrl: String? = null,
        emotionMode: String? = null,
        updateNarratedOnly: Boolean = false,
        translationContents: Map<String, String>? = null,
        translationContentEntries: Map<String, TranslationContentEntryDto>? = null,
        convertPromptUsed: String? = null,
        /** When non-null (JSON property present), updates narration script for [language] after save. */
        narratedContentPatch: String? = null,
        /** When null, keep existing master row; when non-null (including empty list), replace normalized prompts. */
        parentDiscussionPrompts: List<String>? = null,
        /** When null, keep existing; when non-null (including blank), replace or clear. */
        parentContentNote: String? = null,
        /** When null, keep existing; when non-null (including blank), replace or clear. */
        speakAlongPrompt: String? = null,
    ): LibraryStory? {
        val startedNs = System.nanoTime()
        fun elapsedMs(): Long = (System.nanoTime() - startedNs) / 1_000_000
        log.info("Library story update id={} status={} updateNarratedOnly={}", id, status, updateNarratedOnly)
        try {
            val existing = repository.findById(id) ?: return null
            val requestedLang = language.trim().lowercase().take(10).ifEmpty { "ta" }
            val effectiveStatus = status.take(20).uppercase().let { if (it in StoryStatus.ALLOWED_FROM_REQUEST) it else existing.status }
            if (effectiveStatus == StoryStatus.PUBLISHED) {
                StoryLibraryValidation.validateMinWordCount(content).getOrElse { throw it }
                StoryLibraryValidation.validateScriptForLanguage(content, requestedLang).getOrElse { throw it }
            } else {
                val draftWordCount = content.split(Regex("\\s+")).count { it.isNotBlank() }
                if (draftWordCount < StoryLibraryValidation.MIN_WORD_COUNT) {
                    log.info(
                        "Library story update id={} saved as DRAFT with short content words={} minRequired={}",
                        id,
                        draftWordCount,
                        StoryLibraryValidation.MIN_WORD_COUNT
                    )
                }
            }
            title?.takeIf { it.isNotBlank() }?.let { t ->
                if (repository.existsByTitle(t) && t != existing.title) {
                    throw IllegalArgumentException("A story with this title already exists")
                }
            }
        val wordCount = content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val readingTimeMinutes = (wordCount / 150.0).coerceAtMost(5.0)
        // When content has an Indic script that differs from requested language, migrate master language so edit form
        // loads correctly on next open. Supports ta, hi, te, kn, ml. English has no distinctive script; keep requested.
        val inferredLang = inferLanguageFromScript(content)
        val effectiveLang = if (inferredLang != null && inferredLang != requestedLang) {
            log.info("Library story update id={}: content has {} script, migrating master language {} -> {}", id, inferredLang, requestedLang, inferredLang)
            inferredLang
        } else requestedLang
        val effectiveEmotion = emotionMode?.trim()?.uppercase()?.take(20)
            ?.let { if (it in listOf("CALM", "SOOTHING", "ADVENTUROUS")) it else existing.emotionMode ?: "CALM" }
            ?: (existing.emotionMode ?: "CALM")
        // Preserve existing cover URLs when request sends null/blank so generated AI cover is not lost on publish.
        val effectiveCoverImage = coverImageUrl?.takeIf { it.isNotBlank() }?.take(512) ?: existing.coverImageUrl
        val effectiveCoverVideo = coverVideoUrl?.takeIf { it.isNotBlank() }?.take(512) ?: existing.coverVideoUrl
        val contentSame = normalizeForCompare(existing.content) == normalizeForCompare(content)
        val titleSame = normalizeForCompare(existing.title) == normalizeForCompare(title)
        val moralSame = normalizeForCompare(existing.moral) == normalizeForCompare(moral)
        val sourceContentChanged = !(contentSame && titleSame && moralSame)
        val resolvedParentPrompts = when {
            parentDiscussionPrompts == null -> existing.parentDiscussionPrompts
            parentDiscussionPrompts.isEmpty() -> null
            else ->
                parentDiscussionPrompts
                    .mapNotNull { it.trim().takeIf { s -> s.isNotBlank() }?.take(400) }
                    .take(10)
                    .takeIf { it.isNotEmpty() }
        }
        val resolvedParentNote =
            if (parentContentNote == null) {
                existing.parentContentNote
            } else {
                parentContentNote.trim().take(4000).takeIf { it.isNotBlank() }
            }
        val resolvedSpeakAlong =
            if (speakAlongPrompt == null) {
                existing.speakAlongPrompt
            } else {
                speakAlongPrompt.trim().take(500).takeIf { it.isNotBlank() }
            }
        val updated = LibraryStory(
            id = existing.id,
            title = title?.takeIf { it.isNotBlank() },
            content = content,
            theme = theme.trim().take(100),
            category = category?.trim()?.take(100) ?: existing.category ?: theme.trim().take(100),
            language = effectiveLang,
            age = age.coerceIn(1, 12),
            childName = childName.ifBlank { "Child" }.take(255),
            wordCount = wordCount,
            readingTimeMinutes = readingTimeMinutes,
            moral = moral?.takeIf { it.isNotBlank() },
            audioFileUrl = existing.audioFileUrl,
            status = effectiveStatus,
            coverImageUrl = effectiveCoverImage,
            coverVideoUrl = effectiveCoverVideo,
            createdAt = existing.createdAt,
            updatedAt = java.time.Instant.now(),
            storyOwner = existing.storyOwner,
            convertPromptUsed = convertPromptUsed?.trim()?.takeIf { it.isNotBlank() }?.take(8000) ?: existing.convertPromptUsed,
            emotionMode = effectiveEmotion,
            narrationApprovedAt = existing.narrationApprovedAt,
            reviewNotes = existing.reviewNotes,
            rejectMarkedAt = existing.rejectMarkedAt,
            regeneratePromptLocked = existing.regeneratePromptLocked,
            regeneratePromptLockApproved = existing.regeneratePromptLockApproved,
            regeneratePromptUnlockRequestedAt = existing.regeneratePromptUnlockRequestedAt,
            deletedAt = existing.deletedAt,
            parentDiscussionPrompts = resolvedParentPrompts,
            parentContentNote = resolvedParentNote,
            speakAlongPrompt = resolvedSpeakAlong,
        )
        val metadataChanged =
            normalizeForCompare(existing.theme) != normalizeForCompare(updated.theme) ||
                normalizeForCompare(existing.category) != normalizeForCompare(updated.category) ||
                normalizeForCompare(existing.coverImageUrl) != normalizeForCompare(updated.coverImageUrl) ||
                normalizeForCompare(existing.coverVideoUrl) != normalizeForCompare(updated.coverVideoUrl) ||
                normalizeForCompare(existing.emotionMode) != normalizeForCompare(updated.emotionMode) ||
                existing.age != updated.age ||
                normalizeForCompare(existing.childName) != normalizeForCompare(updated.childName) ||
                existing.parentDiscussionPrompts != updated.parentDiscussionPrompts ||
                normalizeForCompare(existing.parentContentNote) != normalizeForCompare(updated.parentContentNote) ||
                normalizeForCompare(existing.speakAlongPrompt) != normalizeForCompare(updated.speakAlongPrompt)
        // Persist `library_stories` last: updating the master row first used to hold that row lock through
        // translation sync, S3 cleanup (submit-for-review), and LLM/TTS windows — blocking other writers and
        // matching admin client timeouts (Generate translations saves draft before rebuild).
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
        val allowedLangs = listOf(sourceLang) + appProperties.translationPipeline.targetLanguages.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val existingTranslationsByLang = storyTranslationRepository.findByMasterStoryId(id).associateBy { it.language }
        // When submitted for review: preserve existing other-language content if request did not send translationContents/translationContentEntries
        val toApply: Map<String, TranslationContentEntryDto> = when {
            !translationContentEntries.isNullOrEmpty() -> translationContentEntries.mapValues { (_, v) ->
                TranslationContentEntryDto(
                    v.content.trim().take(50_000),
                    v.title?.trim()?.takeIf { it.isNotBlank() },
                    v.moral?.trim()?.takeIf { it.isNotBlank() }
                )
            }.filter { (_, v) ->
                v.content.isNotBlank() || !v.title.isNullOrBlank() || !v.moral.isNullOrBlank()
            }
            // Legacy: translationContents has only content per lang; do NOT use master title/moral for other languages
            !translationContents.isNullOrEmpty() -> translationContents.mapValues { (lang, c) ->
                val tr = storyTranslationRepository.findByMasterStoryIdAndLanguage(id, lang.trim().lowercase())
                TranslationContentEntryDto(
                    c.trim().take(50_000),
                    tr?.title?.takeIf { it.isNotBlank() },
                    tr?.moral?.takeIf { it.isNotBlank() }
                )
            }.filter { (_, v) -> v.content.isNotBlank() }
            // No implicit "copy all other languages on submit" fallback.
            // Translations now remain in-place during submit-for-review reset, so upserting every language
            // here only adds lock/write pressure and slows Tamil-only submit flows.
            else -> emptyMap()
        }
        val changedTargetLanguages = toApply.mapNotNull { (lang, entry) ->
            val normalizedLang = lang.trim().lowercase().take(10)
            if (normalizedLang.isBlank() || normalizedLang !in allowedLangs || normalizedLang == effectiveLang) return@mapNotNull null
            if (entry.content.isBlank() && entry.title.isNullOrBlank() && entry.moral.isNullOrBlank()) return@mapNotNull null
            val existingTranslation = existingTranslationsByLang[normalizedLang]
            val compareContent = when {
                entry.content.isNotBlank() -> entry.content
                else -> existingTranslation?.content.orEmpty()
            }
            if (compareContent.isBlank() && entry.title.isNullOrBlank() && entry.moral.isNullOrBlank()) return@mapNotNull null
            if (hasTranslationContentChanged(existingTranslation, compareContent, entry.title, entry.moral)) normalizedLang else null
        }.distinct()
        val shouldClearNarrationApproval = effectiveStatus == StoryStatus.PUBLISHED &&
            existing.narrationApprovedAt != null &&
            (
                !appProperties.translationPipeline.keepNarrationApprovalOnMetadataOnlyPublishedUpdate ||
                    sourceContentChanged ||
                    changedTargetLanguages.isNotEmpty()
                )
        val updatedWithApprovalPolicy = if (shouldClearNarrationApproval) {
            updated.copy(narrationApprovedAt = null)
        } else updated
        // When submitted for review: block if pipeline running or content unchanged
        if (effectiveStatus == StoryStatus.PUBLISHED) {
            val claimMaxAge = appProperties.translationPipeline.claimMaxAgeMinutes.coerceIn(5, 60)
            if (progressTracker.clearStuckIfOlderThan(id, claimMaxAge)) {
                log.info(
                    "Cleared stale in-memory pipeline marker for story id={} (age > {} min) before submit-for-review check",
                    id,
                    claimMaxAge
                )
            }
            progressTracker.getProcessingLanguage(id)?.let { lang ->
                throw PipelineRunningException("Pipeline is already running for this story (currently processing $lang). Please wait for it to complete before submitting again.", lang)
            }
            if (existing.status in StoryStatus.CONTENT_SAME_CHECK) {
                if (!sourceContentChanged && changedTargetLanguages.isEmpty() && !metadataChanged) {
                    throw ContentUnchangedException()
                }
            }
            // New review cycle: clear "Have reviewed" per-language flags from the prior cycle.
            libraryStoryLanguageReviewJpaRepository.deleteByLibraryStoryId(id)
            val requiresFullReset = existing.status == StoryStatus.DRAFT || sourceContentChanged
            val audioAfterApproval = appProperties.translationPipeline.audioAfterApproval
            when {
                requiresFullReset && !audioAfterApproval -> {
                    resetPipelineStateForSubmitForReview(id, existing, updatedWithApprovalPolicy)
                }
                requiresFullReset && audioAfterApproval -> {
                    log.info(
                        "Submit for review story id={}: audio-after-approval=true; clearing narration files, translations kept",
                        id
                    )
                    clearAllNarrationAudioForReviewResubmit(id)
                    libraryStoryIllustrationService.deleteReplacedCoverAssets(existing, updatedWithApprovalPolicy)
                }
                changedTargetLanguages.isNotEmpty() -> {
                    invalidateNarrationAudioForLanguages(id, changedTargetLanguages)
                    libraryStoryIllustrationService.deleteReplacedCoverAssets(existing, updatedWithApprovalPolicy)
                }
                else -> {
                    log.info(
                        "Submit for review story id={}: clearing all narration audio for new review cycle (metadata-only or minor resubmit)",
                        id
                    )
                    clearAllNarrationAudioForReviewResubmit(id)
                    libraryStoryIllustrationService.deleteReplacedCoverAssets(existing, updatedWithApprovalPolicy)
                }
            }
            log.info(
                "Library story update id={} submit-review phase complete in {}ms (requiresFullReset={} changedTargetLanguages={})",
                id,
                elapsedMs(),
                requiresFullReset,
                changedTargetLanguages.size
            )
        }
        // Keep `story_translations` in sync with master for [effectiveLang] on every save (draft, published, etc.).
        // Separate transaction commits translation row locks before we update `library_stories` (see [applyAdminTranslationEditsInNewTransaction]).
        storyLibrarySelf.getObject().applyAdminTranslationEditsInNewTransaction(
            id = id,
            effectiveLang = effectiveLang,
            title = title,
            content = content,
            moral = moral,
            toApply = toApply,
            allowedLangs = allowedLangs,
            narratedContentPatch = narratedContentPatch
        )
        log.info(
            "Library story update id={} translation-sync complete in {}ms (effectiveLang={} toApply={})",
            id,
            elapsedMs(),
            effectiveLang,
            toApply.size
        )
            val saved = repository.update(updatedWithApprovalPolicy)
            val totalMs = elapsedMs()
            storyPipelineMetrics.recordAdminStoryUpdateLatency(totalMs, scope = "service", outcome = "success", status = effectiveStatus)
            when {
                totalMs >= updateSlowErrorMs -> log.error(
                    "Library story update id={} VERY_SLOW total={}ms status={} effectiveLang={} toApply={} changedTargetLanguages={}",
                    id, totalMs, effectiveStatus, effectiveLang, toApply.size, changedTargetLanguages.size
                )
                totalMs >= updateSlowWarnMs -> log.warn(
                    "Library story update id={} SLOW total={}ms status={} effectiveLang={} toApply={} changedTargetLanguages={}",
                    id, totalMs, effectiveStatus, effectiveLang, toApply.size, changedTargetLanguages.size
                )
                else -> log.info(
                    "Library story update id={} completed in {}ms status={} effectiveLang={}",
                    id, totalMs, effectiveStatus, effectiveLang
                )
            }
            return saved
        } catch (e: Exception) {
            storyPipelineMetrics.recordAdminStoryUpdateLatency(elapsedMs(), scope = "service", outcome = "error", status = status)
            log.error(
                "Library story update id={} FAILED after {}ms status={} error={}",
                id, elapsedMs(), status, e.message, e
            )
            throw e
        }
    }

    /**
     * Search library stories by theme or title (case-insensitive).
     * Language filter applied on master `library_stories.language` (works for any catalogue primary language).
     */
    fun search(query: String, language: String, page: Int, size: Int): Page<LibraryStoryResponse> {
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

    /**
     * Batch pipeline status for multiple stories. Reduces N HTTP requests to 1 when admin polls.
     */
    fun getPipelineStatusByStoryIds(masterStoryIds: List<Long>): Map<Long, Map<String, String>> {
        if (masterStoryIds.isEmpty()) return emptyMap()
        return masterStoryIds.associateWith { getPipelineStatusByStoryId(it) }
            .filterValues { it.isNotEmpty() }
    }

    /**
     * Returns pipeline status for admin UX.
     * Keys: per-language (ta, hi, en, ...), processing (current lang), overallStatus, progress.
     * overallStatus: COMPLETED (all audio done) | TRANSLATING_LANGUAGES | TTS_PROCESSING | FINALIZING_STORY | FAILED
     * progress: 0-100
     * Uses batch fetches to avoid N+1 queries when polling multiple stories.
     */
    fun getPipelineStatusByStoryId(masterStoryId: Long): Map<String, String> {
        val story = repository.findById(masterStoryId) ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()

        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        val supportedLangs = (listOf(sourceLang) + appProperties.translationPipeline.targetLanguages
            .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }).distinct()

        val storyMasterLang = story.language.trim().lowercase().take(10).ifEmpty { sourceLang }
        val masterOnlyNarration = appProperties.translationPipeline.masterOnlyNarration

        val translationIds = translations.map { it.id }
        val narrationByTranslationId = narrationAudioRepository.findByTranslationIdInAndVoiceProfile(translationIds, "default")
            .filter { it.status == NarrationAudioStatus.READY && !it.audioUrl.isNullOrBlank() }
            .associateBy { it.translationId }

        fun masterTranslation() = translations.find { it.language.equals(storyMasterLang, ignoreCase = true) }
        fun masterHasPlayableAudio(): Boolean {
            if (!story.audioFileUrl.isNullOrBlank()) return true
            val mt = masterTranslation() ?: return false
            return narrationByTranslationId[mt.id] != null
        }

        var completedCount = 0
        var hasFailed = false
        var inProgress = false

        for (lang in supportedLangs) {
            val translation = translations.find { it.language.equals(lang, ignoreCase = true) }
            val hasNarrationOnRow = translation != null && narrationByTranslationId[translation.id] != null
            val hasLegacyThisMasterLang = lang.equals(storyMasterLang, ignoreCase = true) && !story.audioFileUrl.isNullOrBlank()
            val hasLegacySourceFallback = !masterOnlyNarration && lang.equals(sourceLang, ignoreCase = true) &&
                !story.audioFileUrl.isNullOrBlank()
            val hasAudio = when {
                masterOnlyNarration && !lang.equals(storyMasterLang, ignoreCase = true) -> masterHasPlayableAudio()
                else -> hasNarrationOnRow || hasLegacyThisMasterLang || hasLegacySourceFallback
            }
            val shouldReconcileCompleted =
                hasAudio && translation != null && translation.status != TranslationPipelineStatus.COMPLETED &&
                    (hasNarrationOnRow || hasLegacyThisMasterLang || hasLegacySourceFallback)
            if (shouldReconcileCompleted) {
                // Keep status endpoint read-only; do not mutate translation rows during admin polling.
                log.debug(
                    "Pipeline status drift detected masterStoryId={} lang={} dbStatus={} computed=COMPLETED",
                    masterStoryId,
                    lang,
                    translation.status
                )
            }
            val ts = translation?.status?.name ?: "PENDING"
            // Only show COMPLETED when we have actual audio (narration row or legacy URL). If translation says COMPLETED
            // but there is no narration row (e.g. stale after S3 delete or clear), show PENDING so we don't report Done/100%.
            val status = if (hasAudio) "COMPLETED" else if (ts == "COMPLETED") "PENDING" else ts
            if (translation?.status?.isFailed() == true) {
                if (!masterOnlyNarration || lang.equals(storyMasterLang, ignoreCase = true)) {
                    hasFailed = true
                }
            }
            val errorSuffix = if (translation?.lastError != null && translation.status?.isFailed() == true) {
                val retryInfo = if (translation.retryCount > 0) " (${translation.retryCount} retries)" else ""
                " — ${sanitizeErrorForDisplay(translation.lastError)}$retryInfo"
            } else ""
            result[lang] = status + errorSuffix
            if (status == "COMPLETED") completedCount++
            if (status in listOf("TRANSLATING", "REWRITING", "TTS_PROCESSING")) inProgress = true
        }
        val trackerLang = progressTracker.getProcessingLanguage(masterStoryId)
        val pipelineNotFinished = completedCount < supportedLangs.size && !hasFailed
        // Do NOT clear the tracker just because no row is TRANSLATING/TTS right now — between sequential
        // languages (or thread handoffs) every row can briefly be PENDING/COMPLETED while the run continues.
        trackerLang?.let { processingLang ->
            val hasDbInProgress = translations.any { t ->
                t.status in setOf(
                    TranslationPipelineStatus.TRANSLATING,
                    TranslationPipelineStatus.REWRITING,
                    TranslationPipelineStatus.TTS_PROCESSING
                ) &&
                    (!masterOnlyNarration || t.language.equals(storyMasterLang, ignoreCase = true))
            }
            if (hasDbInProgress || pipelineNotFinished) {
                result["processing"] = processingLang
                if (pipelineNotFinished) inProgress = true
            }
        }
        if (completedCount == supportedLangs.size && trackerLang != null) {
            progressTracker.clearProcessing(masterStoryId)
        }

        val progress = if (supportedLangs.isEmpty()) 0 else (completedCount * 100) / supportedLangs.size
        result["progress"] = progress.toString()
        // Duration (seconds): prefer story master row, then configured source language.
        val durationTranslation = masterTranslation()
            ?: translations.find { it.language.equals(sourceLang, ignoreCase = true) }
        val sourceNarration = durationTranslation?.let { narrationByTranslationId[it.id] }
        val maxCompletedDuration = narrationByTranslationId.values.map { it.durationSeconds }.maxOrNull()?.takeIf { it > 0 } ?: 0
        val sourceDur = sourceNarration?.durationSeconds ?: 0
        val durationSeconds = when {
            sourceDur > 0 -> sourceDur
            maxCompletedDuration > 0 -> maxCompletedDuration
            else -> 0
        }
        if (durationSeconds > 0) result["durationSeconds"] = durationSeconds.toString()
        // Only flag truncation when the pipeline persisted truncation_warning (reliable). A legacy on-the-fly
        // heuristic using reading_time_minutes vs duration produced false positives for many languages.
        val shortAudioLangs = supportedLangs.filter { lang ->
            if (masterOnlyNarration && !lang.equals(storyMasterLang, ignoreCase = true)) return@filter false
            val translation = translations.find { it.language.equals(lang, ignoreCase = true) } ?: return@filter false
            val narration = narrationByTranslationId[translation.id] ?: return@filter false
            narration.truncationWarning
        }
        if (shortAudioLangs.isNotEmpty()) {
            result["audioCoverageWarnings"] = shortAudioLangs.joinToString(",")
        }
        // Timestamp when audio was last generated/regenerated (IST) from story_narration_audio.created_at
        val latestGenerated = if (masterOnlyNarration) {
            masterTranslation()?.let { narrationByTranslationId[it.id]?.createdAt }
        } else {
            narrationByTranslationId.values.map { it.createdAt }.maxOrNull()
        }
        latestGenerated?.let { instant ->
            val istFormatted = instant.atZone(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"))
            result["generatedAtIst"] = "$istFormatted IST"
        }
        // HaveReviewed flags: each (library_story_id, language) row = reviewed. Check all required langs have a row.
        val reviewRows = libraryStoryLanguageReviewJpaRepository.findByLibraryStoryId(masterStoryId)
        val reviewedLangs = reviewRows.map { it.language.trim().lowercase() }.toSet()
        result["reviewedLanguages"] = reviewedLangs.joinToString(",")
        val staleReviewed = reviewRows.mapNotNull { row ->
            val lang = row.language.trim().lowercase()
            val stored = row.contentFingerprint?.trim()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val current = fingerprintLanguageContent(story, lang, sourceLang, translations)
            if (current != stored) lang else null
        }.toSet()
        if (staleReviewed.isNotEmpty()) {
            result["reviewStaleLanguages"] = staleReviewed.joinToString(",")
        }
        val supportedLangsLower = supportedLangs.map { it.trim().lowercase() }
        val missingLangs = supportedLangsLower.filter { it !in reviewedLangs }
        // Require every language reviewed and no fingerprint drift (content edited after review).
        val allLanguagesReviewed = supportedLangsLower.isNotEmpty() &&
            missingLangs.isEmpty() &&
            staleReviewed.isEmpty()
        result["allLanguagesReviewed"] = allLanguagesReviewed.toString()
        // Log only when noteworthy: in progress, has reviewed langs, or fully ready (avoids 20 lines per batch poll)
        if (inProgress || reviewedLangs.isNotEmpty() || allLanguagesReviewed) {
            log.debug("Pipeline status storyId={} allReviewed={} inProgress={} reviewed={}",
                masterStoryId, allLanguagesReviewed, inProgress, reviewedLangs)
        }
        val langStatuses = supportedLangs.mapNotNull { result[it] }
        result["overallStatus"] = when {
            hasFailed -> "FAILED"
            completedCount == supportedLangs.size -> "COMPLETED"
            inProgress -> when {
                langStatuses.any { it.startsWith("TTS_PROCESSING") } -> "TTS_PROCESSING"
                langStatuses.any { it.startsWith("REWRITING") } || langStatuses.any { it.startsWith("TRANSLATING") } ->
                    "TRANSLATING_LANGUAGES"
                completedCount > 0 -> "TTS_PROCESSING" // between languages: audio phase, not global "Queued"
                else -> "TRANSLATING_LANGUAGES"
            }
            else -> "PENDING"
        }
        // Partial failure summary: count of failed languages for enterprise visibility
        val failedCount = supportedLangs.count { lang ->
            if (masterOnlyNarration && !lang.equals(storyMasterLang, ignoreCase = true)) return@count false
            val translation = translations.find { it.language.equals(lang, ignoreCase = true) } ?: return@count false
            translation.status?.isFailed() == true
        }
        if (failedCount > 0) result["failedLanguagesCount"] = failedCount.toString()
        return result
    }

    /** True if any story pipeline is currently running (translate/TTS). Used to disable Story for review UI. */
    fun isPipelineActive(): Boolean = progressTracker.hasAnyProcessing()

    /** Active pipelines: storyId -> language code. For admin UI banner ("Generating X for story #Y"). */
    fun getActiveStories(): Map<Long, String> = progressTracker.getActiveStories()

    /**
     * Returns active pipeline language for a story when it is truly running.
     * Guards against stale in-memory tracker claims by verifying DB in-progress statuses.
     * Keeps a short grace window for just-started runs before status rows are updated.
     */
    fun getActivePipelineLanguageIfRunning(storyId: Long): String? {
        val language = progressTracker.getProcessingLanguage(storyId) ?: return null
        val inProgressStatuses = setOf(
            TranslationPipelineStatus.TRANSLATING,
            TranslationPipelineStatus.REWRITING,
            TranslationPipelineStatus.TTS_PROCESSING
        )
        val hasDbInProgress = storyTranslationRepository.findByMasterStoryId(storyId)
            .any { it.status in inProgressStatuses }
        if (hasDbInProgress) return language
        val ageMinutes = progressTracker.getProcessingAgeMinutes(storyId) ?: 0
        if (ageMinutes <= 1) return language
        progressTracker.clearProcessing(storyId)
        log.warn(
            "Cleared stale pipeline tracker claim before update guard storyId={} lang={} age={}m (no DB in-progress statuses)",
            storyId,
            language,
            ageMinutes
        )
        return null
    }

    /** Clear stuck pipeline entry if older than maxAgeMinutes. Returns true if cleared. Use when banner shows no progress. */
    fun clearStuckPipeline(storyId: Long, maxAgeMinutes: Int? = null): Boolean {
        val age = maxAgeMinutes ?: appProperties.translationPipeline.claimMaxAgeMinutes.coerceIn(5, 60)
        return progressTracker.clearStuckIfOlderThan(storyId, age)
    }

    /** Age in minutes for a story's current pipeline run, or null if not tracked. */
    fun getPipelineAgeMinutes(storyId: Long): Int? = progressTracker.getProcessingAgeMinutes(storyId)

    /**
     * Stories that need pipeline attention: failed translations or stuck in-progress.
     * For admin "Pipeline triage" menu. Includes TRANSLATING/REWRITING/TTS_PROCESSING
     * so stuck stories (e.g. banner shows "no progress") appear here.
     */
    fun getStoriesWithIssues(): List<StoryWithIssues> {
        val failedStatuses = listOf(
            TranslationPipelineStatus.TRANSLATION_FAILED,
            TranslationPipelineStatus.REWRITE_FAILED,
            TranslationPipelineStatus.TTS_FAILED
        )
        val stuckStatuses = listOf(
            TranslationPipelineStatus.TRANSLATING,
            TranslationPipelineStatus.REWRITING,
            TranslationPipelineStatus.TTS_PROCESSING
        )
        val failedTranslations = storyTranslationRepository.findByStatusIn(failedStatuses)
        val stuckTranslations = storyTranslationRepository.findByStatusIn(stuckStatuses)
        val allTranslations = (failedTranslations + stuckTranslations).distinctBy { "${it.masterStoryId}:${it.language}" }
        val stuckThreshold = appProperties.translationPipeline.claimMaxAgeMinutes.coerceIn(5, 60)
        val activeStories = progressTracker.getActiveStories()
        val stuckFromBanner = progressTracker.getActiveStories().filter { (id, _) ->
            (progressTracker.getProcessingAgeMinutes(id) ?: 0) >= stuckThreshold
        }
        val storyIdsFromTranslations = allTranslations.map { it.masterStoryId }.toSet()
        val storyIdsFromBanner = stuckFromBanner.keys
        val storyIdsFromActive = activeStories.keys
        val allStoryIds = storyIdsFromTranslations + storyIdsFromBanner + storyIdsFromActive
        if (allStoryIds.isEmpty()) return emptyList()
        val byMaster = allTranslations.groupBy { it.masterStoryId }
        val masterOnlyNarration = appProperties.translationPipeline.masterOnlyNarration
        return allStoryIds.distinct().mapNotNull { masterId ->
            val story = repository.findById(masterId) ?: return@mapNotNull null
            val storyMasterLang = story.language.trim().lowercase().take(10).ifEmpty {
                appProperties.translationPipeline.sourceLanguage.trim().lowercase()
            }
            val translationIssues = byMaster[masterId]?.mapNotNull { t ->
                if (masterOnlyNarration && !t.language.equals(storyMasterLang, ignoreCase = true)) return@mapNotNull null
                val isFailed = t.status in failedStatuses
                LanguageIssue(
                    language = t.language,
                    status = t.status.name,
                    error = if (isFailed) (sanitizeErrorForDisplay(t.lastError)?.take(200) ?: "Unknown error")
                        else "In progress — may be stuck. Use Clear stuck in banner or Run pipeline."
                )
            } ?: emptyList()
            val bannerStuck = stuckFromBanner[masterId]?.let { lang ->
                if (translationIssues.any { it.language == lang }) null
                else LanguageIssue(language = lang, status = "STUCK", error = "Processing > ${stuckThreshold} min. Clear stuck in banner, then Run pipeline.")
            }
            val runningIssue = activeStories[masterId]?.let { lang ->
                if (translationIssues.any { it.language == lang } || bannerStuck?.language == lang) null
                else LanguageIssue(
                    language = lang,
                    status = "RUNNING",
                    error = "Pipeline is currently running for this story."
                )
            }
            val issues = translationIssues + listOfNotNull(bannerStuck, runningIssue)
            if (issues.isEmpty()) return@mapNotNull null
            StoryWithIssues(
                storyId = masterId,
                title = story.title?.take(80) ?: "Story #$masterId",
                theme = story.theme ?: "",
                status = story.status,
                issues = issues.distinctBy { it.language }
            )
        }.sortedBy { it.storyId }
    }

    /**
     * Mark one or more languages as reviewed for a story (insert or refresh fingerprint when re-clicking "Have reviewed").
     * Enables Approve when all pipeline languages are reviewed (admin uses "Have reviewed" in View modal).
     */
    @Transactional
    fun markLanguagesReviewed(masterStoryId: Long, languages: List<String>) {
        if (languages.isEmpty()) return
        val story = repository.findById(masterStoryId) ?: return
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
        val normalized = languages.map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        val now = Instant.now()
        for (lang in normalized) {
            val fp = fingerprintLanguageContent(story, lang, sourceLang, translations)
            val row = libraryStoryLanguageReviewJpaRepository.findByLibraryStoryIdAndLanguageIgnoreCase(masterStoryId, lang)
            if (row == null) {
                libraryStoryLanguageReviewJpaRepository.save(
                    LibraryStoryLanguageReviewEntity(
                        libraryStoryId = masterStoryId,
                        language = lang,
                        reviewedAt = now,
                        contentFingerprint = fp
                    )
                )
            } else {
                row.reviewedAt = now
                row.contentFingerprint = fp
                libraryStoryLanguageReviewJpaRepository.save(row)
            }
        }
        log.info("Marked/updated languages as reviewed storyId={} languages={}", masterStoryId, normalized)
    }

    /** Dev/debug: return translation details for a story (status, lastError, retryCount). */
    fun getTranslationsForStory(masterStoryId: Long): List<Map<String, Any?>> =
        storyTranslationRepository.findByMasterStoryId(masterStoryId).map { t ->
            mapOf(
                "language" to t.language,
                "status" to t.status?.name,
                "retryCount" to t.retryCount,
                "lastError" to (t.lastError?.take(200)),
                "hasContent" to t.content.isNotBlank()
            )
        }
}
