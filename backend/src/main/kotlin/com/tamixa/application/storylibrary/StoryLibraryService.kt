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
import com.tamixa.application.story.StoryPromptBuilder
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.infrastructure.persistence.FavoriteStoryJpaRepository
import com.tamixa.infrastructure.persistence.LibraryStoryLanguageReviewEntity
import com.tamixa.infrastructure.persistence.LibraryStoryLanguageReviewJpaRepository
import com.tamixa.infrastructure.persistence.StoryAnalyticsJpaRepository
import com.tamixa.infrastructure.persistence.StoryFeedbackJpaRepository
import com.tamixa.infrastructure.persistence.StoryPlaybackPositionJpaRepository
import com.tamixa.domain.TranslationPipelineStatus
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.domain.LibraryStory
import com.tamixa.domain.StoryTranslation
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.observability.PipelineProgressTracker
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.context.annotation.Lazy
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
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
    @Lazy private val storyProcessingService: StoryProcessingService,
    @Autowired(required = false) private val storyModeration: StoryModerationService? = null
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val debugLogPath: Path? = System.getenv("DEBUG_LOG_PATH")?.takeIf { it.isNotBlank() }?.let { Path.of(it) }

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
     * Resolves canonical audio path from story_narration_audio.
     * Ensures mobile and admin always play the same narrated audio.
     */
    private fun resolveAudioFileUrl(masterStoryId: Long, language: String): String? {
        val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(masterStoryId, language) ?: return null
        val narration = narrationAudioRepository.findByTranslationIdAndVoiceProfile(translation.id, "default")
        return if (narration != null && narration.status == NarrationAudioStatus.READY && !narration.audioUrl.isNullOrBlank())
            narration.audioUrl else null
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
        estimatedReadingMinutes: Double? = null
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
        val story = LibraryStory(
            id = 0,
            title = title,
            content = content,
            theme = theme,
            category = category?.trim()?.take(100) ?: theme,
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
            updatedAt = now,
            storyOwner = storyOwner?.trim()?.takeIf { it.isNotBlank() }?.take(255),
            convertPromptUsed = convertPromptUsed?.trim()?.takeIf { it.isNotBlank() }?.take(8000),
            emotionMode = effectiveEmotion,
            narrationApprovedAt = null
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
        eventPublisher.publishLibraryStoryCreated(saved.id, saved.content)
        return saved
    }

    fun findById(id: Long): LibraryStory? = repository.findById(id)

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
     * @param progressCallback When set (e.g. for async jobs), called after each story with current index and result so far.
     */
    fun generateBulkStoriesWithTemplatePrompt(
        languages: List<String>,
        categories: List<String>,
        totalStories: Int,
        publish: Boolean,
        storyOwner: String?,
        progressCallback: ((currentIndex: Int, total: Int, createdCount: Int, failedCount: Int, created: List<Map<String, Any?>>, failed: List<Map<String, Any?>>) -> Unit)? = null
    ): Map<String, Any> {
        val safeLanguages = languages.map { effectiveLanguage(it) }.distinct().ifEmpty { listOf("ta") }
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
            val prompt = buildBulkGenerationPrompt(language, category, idx)
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

                storyModeration?.moderateBeforeSave(
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

    private fun buildBulkGenerationPrompt(language: String, category: String, index: Int): String {
        val combinedSituation = combinedSituations[index % combinedSituations.size]
        val languageInstruction = bulkPromptLanguageInstruction(language)
        val langReminder = "Remember: write the entire story and all JSON string fields in the output language specified at the top—no mixing of languages."
        val basePrompt = """
$languageInstruction

## Role & mandate
You are Tamixa's story designer. Generate one publication-ready story for the mobile storytelling app. Quality and safety are non-negotiable. Every story must be suitable for TTS, family listening, and Indian family-viewing standards.

## Primary structure & clarity
English is the structural reference. When the output language is not English, use the same sentence clarity (short, clear sentences; one idea per sentence; clear subject and action), then write in the target language so every sentence stays clear and easy to narrate.
• One idea per sentence. Clear subject and verb. Short to medium length. No run-ons. Natural connectors (Then… So… But… or equivalent). Every sentence easy to read aloud in one pass.
• Vocabulary: varied, precise, age-appropriate; everyday and native terms; warm storytelling tone; no jargon.

Category: {category}
Combined Situation: {combinedSituation}

## Quality bar
• Publication-ready: prose that would pass editorial review. Intellectually rich: thought-provoking, meaningful; moral that feels earned.
• Mild conflict resolved peacefully; positive, satisfying ending. Title: catchy and specific (not generic like "A Friendship Story"). Moral: one short sentence; positive value; age-appropriate; not preachy.
• Appeal to both children and adults—engaging for kids, satisfying and thoughtful for elders. Layered meaning, subtle lessons (kindness, courage, honesty, belonging).

## Tone & voice (Tamixa voice)
• Magical, comforting, and joyful. Warm, friendly, emotionally gentle. Like a trusted parent, teacher, or grandparent. No sarcasm, cynicism, or fear.
• Narration: short blocks (1–3 sentences); natural transitions ("Once upon a time…", "One day…", "Then…", "After that…" or equivalent). Dialogue simple and sparse; every sentence natural when read aloud.

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

## Length & output
• 600–1500 words (5–15 min narration). Estimated duration in JSON as appropriate (e.g. "10 min" or numeric seconds).
$langReminder
Return only valid JSON. No markdown, no code fence, no commentary. All string values in the output language. story_text: Storytelling Script with tone markers; magical, comforting, joyful.

{
"title": "",
"category": "",
"theme": "",
"story_text": "",
"moral": "",
"estimated_duration": ""
}
""".trimIndent()
        return basePrompt
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

    fun findByIdAndLanguage(id: Long, language: String): LibraryStoryResponse? {
        val effectiveLang = effectiveLanguage(language)
        return when {
            effectiveLang == "ta" -> {
                val master = repository.findById(id) ?: return null
                val taTranslation = storyTranslationRepository.findByMasterStoryIdAndLanguage(id, effectiveLang)
                val displayContent = taTranslation?.content?.takeIf { it.isNotBlank() } ?: master.content
                val displayTitle = taTranslation?.title?.takeIf { it.isNotBlank() } ?: master.title
                val narratedContent = taTranslation?.let { narrationScriptRepository.findByTranslationId(it.id)?.scriptText?.takeIf { it.isNotBlank() } }
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
                    wordCount = taTranslation?.wordCount ?: master.wordCount,
                    readingTimeMinutes = taTranslation?.readingTimeMinutes ?: master.readingTimeMinutes,
                    moral = taTranslation?.moral?.takeIf { it.isNotBlank() } ?: master.moral,
                    audioFileUrl = canonicalAudio,
                    narratedContent = narratedContent
                )
            }
            else -> {
                val master = repository.findById(id) ?: return null
                val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(id, effectiveLang)
                val contentToServe = translation?.content?.takeIf { it.isNotBlank() }
                if (translation != null && !contentToServe.isNullOrBlank()) {
                    log.debug("Serving translation id={} lang={} translationId={}", id, effectiveLang, translation.id)
                    val narratedContent = narrationScriptRepository.findByTranslationId(translation.id)?.scriptText?.takeIf { it.isNotBlank() }
                    return LibraryStoryResponse(
                        id = master.id,
                        title = rejectIfTamilWhenNotTa(translation.title, effectiveLang) ?: "",
                        content = translation.content,
                        theme = master.theme,
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
                        narratedContent = narratedContent
                    )
                }
                // No row or blank content: return master with empty content for this language so admin can open form and save (creates/updates row)
                val narratedContent = translation?.let { narrationScriptRepository.findByTranslationId(it.id)?.scriptText?.takeIf { it.isNotBlank() } }
                master.toResponse(
                    coverImageUrlResolver.resolveCoverPath(master.coverImageUrl),
                    coverImageUrlResolver.resolveCoverVideoPath(master.coverVideoUrl)
                ).copy(
                    title = rejectIfTamilWhenNotTa(translation?.title, effectiveLang) ?: "",
                    content = translation?.content ?: "",
                    moral = rejectIfTamilWhenNotTa(translation?.moral, effectiveLang) ?: "",
                    wordCount = translation?.wordCount ?: 0,
                    readingTimeMinutes = translation?.readingTimeMinutes ?: 0.0,
                    language = effectiveLang,
                    audioFileUrl = null,
                    narratedContent = narratedContent
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
            effectiveLang == "ta" -> repository.findListingByLanguage("ta", pageable).map { listing ->
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
            effectiveLang == "ta" -> repository.findListingByLanguageAndNarrationApproved("ta", pageable).map { listing ->
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
                val translations = storyTranslationRepository.findListingByLanguageAndMasterNarrationApproved(effectiveLang, pageable)
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
     * Tamil: from master. Other languages: from masters that have approved translation.
     * Used by GET /stories/library/categories.
     */
    fun getCategories(language: String): List<String> {
        val effectiveLang = effectiveLanguage(language)
        return if (effectiveLang == "ta") {
            repository.findDistinctThemesByNarrationApproved("ta")
        } else {
            repository.findDistinctThemesByNarrationApprovedAndTranslationLanguage(effectiveLang)
        }
    }

    /**
     * Same as findByLanguage but only stories approved for delivery (narrationApprovedAt set).
     * Optional theme filter (category). Used by the public API so only human-verified stories appear on the app.
     */
    fun findByLanguageApprovedOnly(language: String, page: Int, size: Int, theme: String? = null): Page<LibraryStoryResponse> {
        val effectiveLang = effectiveLanguage(language)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
        val effectiveTheme = theme?.trim()?.takeIf { it.isNotBlank() }

        return when {
            effectiveLang == "ta" -> {
                val pageResult = if (effectiveTheme != null)
                    repository.findByLanguageAndNarrationApprovedAndTheme("ta", effectiveTheme, pageable)
                else
                    repository.findByLanguageAndNarrationApproved("ta", pageable)
                pageResult.map {
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
            }
            else -> {
                val translations = if (effectiveTheme != null)
                    storyTranslationRepository.findByLanguageAndMasterNarrationApprovedAndTheme(effectiveLang, effectiveTheme, pageable)
                else
                    storyTranslationRepository.findByLanguageAndMasterNarrationApproved(effectiveLang, pageable)
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
                        narrationApprovedAt = master.narrationApprovedAt
                    )
                }
                PageImpl(content, translations.pageable, translations.totalElements)
            }
        }
    }

    /**
     * Tamil: from master. Other languages: from story_translations joined with master + audio.
     * Backward compatible: Tamil serving unchanged.
     */
    fun findByLanguage(language: String, page: Int, size: Int): Page<LibraryStoryResponse> {
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
                        narrationApprovedAt = master.narrationApprovedAt
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

    fun findAllByStatus(status: String?, page: Int, size: Int): Page<LibraryStory> {
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
        log.info("Story id={} sent back for changes", id)
        return true
    }

    /**
     * Reject story (final rejection in review flow). Valid when in review queue (PUBLISHED, PROCESSING, READY) and narrationApprovedAt=null.
     */
    @Transactional
    fun reject(id: Long, notes: String?): Boolean {
        val story = repository.findById(id) ?: return false
        if (story.status !in reviewQueueStatuses || story.narrationApprovedAt != null) return false
        repository.updateStatusAndReviewNotes(id, StoryStatus.REJECTED, notes?.take(2000))
        repository.updateRejectMarkedAt(id, null)
        libraryStoryLanguageReviewJpaRepository.deleteByLibraryStoryId(id)
        log.info("Story id={} rejected", id)
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
     * Pipeline is not triggered—content/audio already exists from Submit for review.
     * Valid only when story is in review queue (PUBLISHED, PROCESSING, READY) and not yet approved.
     */
    @Transactional
    fun approveNarration(id: Long): Boolean {
        val story = repository.findById(id) ?: return false
        if (story.status !in reviewQueueStatuses || story.narrationApprovedAt != null) return false
        repository.updateNarrationApprovedAt(id, java.time.Instant.now())
        repository.updateStatus(id, StoryStatus.PUBLISHED)
        repository.updateRejectMarkedAt(id, null)
        log.info("Narration approved for story id={}; story is now published and visible on the app", id)
        return true
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
            status = existing.status,
            retryCount = existing.retryCount,
            lastError = existing.lastError,
            narrationApprovedAt = existing.narrationApprovedAt
        )
        storyTranslationRepository.save(updated)
        log.info("Translation updated masterStoryId={} language={}", masterStoryId, effectiveLang)
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
        var totalDeleted = 0
        for (lang in languages.distinct()) {
            val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(masterStoryId, lang) ?: continue
            val narrationRows = narrationAudioRepository.findAllByTranslationId(translation.id)
            val s3Keys = narrationRows.mapNotNull { row ->
                val u = row.audioUrl ?: return@mapNotNull null
                when {
                    u.startsWith("stories/") -> u
                    "/stories/" in u -> u.substringAfter("/stories/").let { "stories/$it" }
                    else -> null
                }
            }.toSet()
            if (s3Client != null && s3Keys.isNotEmpty() && appProperties.storage.type == "s3") {
                val bucket = appProperties.storage.effectiveS3Bucket
                for (key in s3Keys) {
                    try {
                        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
                    } catch (e: Exception) {
                        log.warn("S3 delete failed for narration key={}: {}", key, e.message)
                    }
                }
            }
            narrationAudioRepository.deleteByTranslationId(translation.id)
            storyTranslationRepository.atomicStatusUpdate(translation.id, TranslationPipelineStatus.PENDING, null)
            totalDeleted += narrationRows.size
        }
        if (sourceLang in languages) {
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
     * Delete a library story and all related data (favorites, family voices, analytics, playback, feedback).
     * Cascades to story_translations, story_narration_audio via DB.
     */
    @Transactional
    fun deleteById(id: Long): Boolean {
        if (repository.findById(id) == null) return false
        cleanupRelatedData(id)
        repository.deleteById(id)
        log.info("Library story deleted id={}", id)
        return true
    }

    /**
     * Bulk delete library stories. Returns count of deleted stories.
     */
    @Transactional
    fun deleteByIds(ids: List<Long>): Int {
        if (ids.isEmpty()) return 0
        val existing = ids.filter { repository.findById(it) != null }
        existing.forEach { cleanupRelatedData(it) }
        existing.forEach { repository.deleteById(it) }
        log.info("Library stories bulk deleted ids={} count={}", existing, existing.size)
        return existing.size
    }

    private fun cleanupRelatedData(storyId: Long) {
        // Do NOT delete family voice recordings: they are the parent's voice profile used for any story.
        favoriteStoryJpaRepository.deleteByStoryIdAndStorySource(storyId, "library")
        storyAnalyticsJpaRepository.deleteByStoryIdAndStorySource(storyId, "library")
        storyPlaybackPositionJpaRepository.deleteByStoryIdAndStorySource(storyId, "library")
        storyFeedbackJpaRepository.deleteByStoryIdAndStorySource(storyId, "library")
    }

    /**
     * Reset pipeline state when story is submitted for review (first, second, or subsequent time).
     * Clears translations and narration audio so pipeline status shows PENDING until approval runs the pipeline.
     * Does not change library story status (stays PUBLISHED for review queue).
     */
    private fun resetPipelineStateForSubmitForReview(masterStoryId: Long) {
        progressTracker.clearProcessing(masterStoryId)
        val translations = storyTranslationRepository.findByMasterStoryId(masterStoryId)
        // #region agent log
        debugLog(
            runId = "submit-review-latency",
            hypothesisId = "H3",
            location = "StoryLibraryService.kt:resetPipelineStateForSubmitForReview:start",
            message = "reset pipeline state starting",
            data = mapOf(
                "storyId" to masterStoryId,
                "translationsBeforeDelete" to translations.size
            )
        )
        // #endregion
        translations.forEach { narrationAudioRepository.deleteByTranslationId(it.id) }
        storyTranslationRepository.deleteByMasterStoryId(masterStoryId)
        repository.clearAudioUrl(masterStoryId)
        // #region agent log
        debugLog(
            runId = "submit-review-latency",
            hypothesisId = "H3",
            location = "StoryLibraryService.kt:resetPipelineStateForSubmitForReview:done",
            message = "reset pipeline state completed",
            data = mapOf("storyId" to masterStoryId)
        )
        // #endregion
        log.info("Pipeline state reset for story id={} (submit for review); status will show PENDING until approval", masterStoryId)
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
        convertPromptUsed: String? = null
    ): LibraryStory? {
        log.info("Library story update id={} status={} updateNarratedOnly={}", id, status, updateNarratedOnly)
        val existing = repository.findById(id) ?: return null
        StoryLibraryValidation.validateMinWordCount(content).getOrElse { throw it }
        if (language.trim().lowercase() == "ta") {
            StoryLibraryValidation.validateTamilScript(content).getOrElse { throw it }
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
        val requestedLang = language.trim().lowercase().take(10).ifEmpty { "ta" }
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
        val effectiveStatus = status.take(20).uppercase().let { if (it in StoryStatus.ALLOWED_FROM_REQUEST) it else existing.status }
        // When submitting for review (status PUBLISHED), clear approval so the story reappears in Story for review
        val effectiveNarrationApprovedAt = if (effectiveStatus == StoryStatus.PUBLISHED) null else existing.narrationApprovedAt
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
            narrationApprovedAt = effectiveNarrationApprovedAt
        )
        val saved = repository.update(updated)
        val sourceLang = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
        val allowedLangs = listOf(sourceLang) + appProperties.translationPipeline.targetLanguages.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val existingTranslationsByLang = storyTranslationRepository.findByMasterStoryId(id).associateBy { it.language }
        // When submitted for review: preserve existing other-language content if request did not send translationContents/translationContentEntries
        data class LangContent(val content: String, val title: String?, val moral: String?)
        val toApply: Map<String, LangContent> = when {
            !translationContentEntries.isNullOrEmpty() -> translationContentEntries.mapValues { (_, v) ->
                LangContent(v.content.trim().take(50_000), v.title?.trim()?.takeIf { it.isNotBlank() }, v.moral?.trim()?.takeIf { it.isNotBlank() })
            }.filter { (_, v) -> v.content.isNotBlank() }
            // Legacy: translationContents has only content per lang; do NOT use master title/moral for other languages
            !translationContents.isNullOrEmpty() -> translationContents.mapValues { (lang, c) ->
                val existing = storyTranslationRepository.findByMasterStoryIdAndLanguage(id, lang.trim().lowercase())
                LangContent(
                    c.trim().take(50_000),
                    existing?.title?.takeIf { it.isNotBlank() },
                    existing?.moral?.takeIf { it.isNotBlank() }
                )
            }.filter { (_, v) -> v.content.isNotBlank() }
            effectiveStatus == StoryStatus.PUBLISHED -> {
                storyTranslationRepository.findByMasterStoryId(id)
                    .filter { it.language != effectiveLang && it.content?.isNotBlank() == true }
                    .associate { it.language to LangContent(it.content ?: "", it.title, it.moral) }
            }
            else -> emptyMap()
        }
        val changedTargetLanguages = toApply.mapNotNull { (lang, entry) ->
            val normalizedLang = lang.trim().lowercase().take(10)
            if (normalizedLang.isBlank() || normalizedLang !in allowedLangs || normalizedLang == effectiveLang) return@mapNotNull null
            if (entry.content.isBlank()) return@mapNotNull null
            val existingTranslation = existingTranslationsByLang[normalizedLang]
            if (hasTranslationContentChanged(existingTranslation, entry.content, entry.title, entry.moral)) normalizedLang else null
        }.distinct()
        // When submitted for review: block if pipeline running or content unchanged
        if (effectiveStatus == StoryStatus.PUBLISHED) {
            progressTracker.getProcessingLanguage(id)?.let { lang ->
                throw PipelineRunningException("Pipeline is already running for this story (currently processing $lang). Please wait for it to complete before submitting again.", lang)
            }
            val contentSame = normalizeForCompare(existing.content) == normalizeForCompare(content)
            val titleSame = normalizeForCompare(existing.title) == normalizeForCompare(title)
            val moralSame = normalizeForCompare(existing.moral) == normalizeForCompare(moral)
            val sourceContentChanged = !(contentSame && titleSame && moralSame)
            if (existing.status in StoryStatus.CONTENT_SAME_CHECK) {
                // #region agent log
                debugLog(
                    runId = "submit-review-latency",
                    hypothesisId = "H1",
                    location = "StoryLibraryService.kt:update:content-check",
                    message = "submit-for-review change detection",
                    data = mapOf(
                        "storyId" to id,
                        "existingStatus" to existing.status,
                        "contentSame" to contentSame,
                        "titleSame" to titleSame,
                        "moralSame" to moralSame,
                        "changedTargetLanguagesCount" to changedTargetLanguages.size,
                        "translationContentEntriesProvided" to (!translationContentEntries.isNullOrEmpty()),
                        "translationContentsProvided" to (!translationContents.isNullOrEmpty())
                    )
                )
                // #endregion
                if (!sourceContentChanged && changedTargetLanguages.isEmpty()) {
                    // #region agent log
                    debugLog(
                        runId = "submit-review-latency",
                        hypothesisId = "H1",
                        location = "StoryLibraryService.kt:update:unchanged-reject",
                        message = "submit-for-review rejected as unchanged",
                        data = mapOf("storyId" to id, "status" to existing.status)
                    )
                    // #endregion
                    throw ContentUnchangedException()
                }
            }
            // #region agent log
            debugLog(
                runId = "submit-review-latency",
                hypothesisId = "H2",
                location = "StoryLibraryService.kt:update:pipeline-select",
                message = "submit-for-review language selection",
                data = mapOf(
                    "storyId" to id,
                    "existingStatus" to existing.status,
                    "effectiveStatus" to effectiveStatus,
                    "sourceContentChanged" to sourceContentChanged,
                    "changedTargetLanguages" to changedTargetLanguages.joinToString(",")
                )
            )
            // #endregion
            val requiresFullReset = existing.status == StoryStatus.DRAFT || sourceContentChanged
            val audioAfterApproval = appProperties.translationPipeline.audioAfterApproval
            if (requiresFullReset && !audioAfterApproval) {
                // #region agent log
                debugLog(
                    runId = "submit-review-latency",
                    hypothesisId = "H3",
                    location = "StoryLibraryService.kt:update:full-reset",
                    message = "submit-for-review full reset selected",
                    data = mapOf(
                        "storyId" to id,
                        "existingStatus" to existing.status,
                        "sourceContentChanged" to sourceContentChanged
                    )
                )
                // #endregion
                resetPipelineStateForSubmitForReview(id)
            } else if (requiresFullReset && audioAfterApproval) {
                log.info("Submit for review story id={}: audio-after-approval=true, skipping pipeline reset (use Story to Speech after approval)", id)
            } else if (changedTargetLanguages.isNotEmpty()) {
                // #region agent log
                debugLog(
                    runId = "submit-review-latency",
                    hypothesisId = "H4",
                    location = "StoryLibraryService.kt:update:partial-invalidate",
                    message = "submit-for-review partial invalidate selected",
                    data = mapOf(
                        "storyId" to id,
                        "languages" to changedTargetLanguages.joinToString(",")
                    )
                )
                // #endregion
                invalidateNarrationAudioForLanguages(id, changedTargetLanguages)
            }
        }
        if (updateNarratedOnly) {
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
        // After reset: update the translation row for the story's language (main form content).
        // Use effectiveLang (may be migrated to ta when content has Tamil) so Hindi-edited-to-Tamil stories
        // get Tamil in the ta translation, and bulk-generated Hindi stories get Hindi in the hi translation.
        if (effectiveStatus == StoryStatus.PUBLISHED) {
            updateTranslationContent(id, effectiveLang, title, content, moral)
        }
        if (toApply.isNotEmpty()) {
            for ((lang, entry) in toApply) {
                val toApplyLang = lang.trim().lowercase().take(10)
                if (toApplyLang.isBlank() || toApplyLang !in allowedLangs || toApplyLang == effectiveLang) continue
                if (entry.content.isBlank()) continue
                updateTranslationContent(id, toApplyLang, entry.title, entry.content, entry.moral)
                log.info("Updated translation content for story id={} lang={}", id, toApplyLang)
            }
        }
        return saved
    }

    /**
     * Search library stories by theme or title (case-insensitive).
     * Language filter applied; for ta uses master, for others uses translation if available.
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

        val translationIds = translations.map { it.id }
        val narrationByTranslationId = narrationAudioRepository.findByTranslationIdInAndVoiceProfile(translationIds, "default")
            .filter { it.status == NarrationAudioStatus.READY && !it.audioUrl.isNullOrBlank() }
            .associateBy { it.translationId }

        var completedCount = 0
        var hasFailed = false
        var inProgress = false

        for (lang in supportedLangs) {
            val translation = translations.find { it.language.equals(lang, ignoreCase = true) }
            val hasNarration = translation != null && narrationByTranslationId[translation.id] != null
            val hasLegacyMaster = lang == sourceLang && !story.audioFileUrl.isNullOrBlank()
            val hasAudio = hasNarration || hasLegacyMaster
            if (hasAudio && translation != null && translation.status != TranslationPipelineStatus.COMPLETED) {
                storyTranslationRepository.atomicStatusUpdate(translation.id, TranslationPipelineStatus.COMPLETED, null)
                log.info("Reconciled translation masterStoryId={} lang={}: status {} -> COMPLETED (audio exists)",
                    masterStoryId, lang, translation.status)
            }
            val ts = translation?.status?.name ?: "PENDING"
            // Only show COMPLETED when we have actual audio (narration row or legacy URL). If translation says COMPLETED
            // but there is no narration row (e.g. stale after S3 delete or clear), show PENDING so we don't report Done/100%.
            val status = if (hasAudio) "COMPLETED" else if (ts == "COMPLETED") "PENDING" else ts
            if (translation?.status?.isFailed() == true) {
                hasFailed = true
            }
            val errorSuffix = if (translation?.lastError != null && translation.status?.isFailed() == true) {
                val retryInfo = if (translation.retryCount > 0) " (${translation.retryCount} retries)" else ""
                " — ${sanitizeErrorForDisplay(translation.lastError)}$retryInfo"
            } else ""
            result[lang] = status + errorSuffix
            if (status == "COMPLETED") completedCount++
            if (status in listOf("TRANSLATING", "REWRITING", "TTS_PROCESSING")) inProgress = true
        }
        progressTracker.getProcessingLanguage(masterStoryId)?.let { processingLang ->
            val hasDbInProgress = translations.any {
                it.status in setOf(
                    TranslationPipelineStatus.TRANSLATING,
                    TranslationPipelineStatus.REWRITING,
                    TranslationPipelineStatus.TTS_PROCESSING
                )
            }
            if (hasDbInProgress) {
                result["processing"] = processingLang
                if (completedCount < supportedLangs.size) inProgress = true
            } else {
                // Recover from stale in-memory tracker entries after crashes/timeouts so UI does not stay "pending/running".
                progressTracker.clearProcessing(masterStoryId)
            }
        }

        val progress = if (supportedLangs.isEmpty()) 0 else (completedCount * 100) / supportedLangs.size
        result["progress"] = progress.toString()
        // Duration in seconds: source lang if available, else max of completed languages
        val sourceNarration = translations.find { it.language.equals(sourceLang, ignoreCase = true) }
            ?.let { narrationByTranslationId[it.id] }
        val durationSeconds = (sourceNarration?.durationSeconds ?: 0).takeIf { it > 0 }
            ?: narrationByTranslationId.values.map { it.durationSeconds }.maxOrNull()?.takeIf { it > 0 } ?: 0
        if (durationSeconds > 0) result["durationSeconds"] = durationSeconds.toString()
        // Audio coverage warnings: read from story_narration_audio.truncation_warning (set at save time).
        // Fallback: for pre-migration rows (truncation_warning=false), compute from duration vs expected.
        val shortAudioLangs = supportedLangs.filter { lang ->
            val translation = translations.find { it.language.equals(lang, ignoreCase = true) } ?: return@filter false
            val narration = narrationByTranslationId[translation.id] ?: return@filter false
            if (narration.truncationWarning) return@filter true
            // Legacy rows: compute on-the-fly when stored flag is false
            val audioSecs = narration.durationSeconds
            if (audioSecs <= 0) return@filter false
            val expectedSecs = (translation.readingTimeMinutes * 60).toInt().coerceAtLeast(10)
            audioSecs < expectedSecs * 0.5
        }
        if (shortAudioLangs.isNotEmpty()) {
            result["audioCoverageWarnings"] = shortAudioLangs.joinToString(",")
        }
        // Timestamp when audio was last generated/regenerated (IST) from story_narration_audio.created_at
        val latestGenerated = narrationByTranslationId.values.map { it.createdAt }.maxOrNull()
        latestGenerated?.let { instant ->
            val istFormatted = instant.atZone(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"))
            result["generatedAtIst"] = "$istFormatted IST"
        }
        // HaveReviewed flags: each (library_story_id, language) row = reviewed. Check all required langs have a row.
        val reviewedLangs = libraryStoryLanguageReviewJpaRepository.findByLibraryStoryId(masterStoryId)
            .map { it.language }.map { it.trim().lowercase() }.toSet()
        result["reviewedLanguages"] = reviewedLangs.joinToString(",")
        val supportedLangsLower = supportedLangs.map { it.trim().lowercase() }
        val missingLangs = supportedLangsLower.filter { it !in reviewedLangs }
        val allLanguagesReviewed = supportedLangsLower.isNotEmpty() && missingLangs.isEmpty()
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
                langStatuses.any { it.startsWith("REWRITING") } -> "TRANSLATING_LANGUAGES"
                else -> "TRANSLATING_LANGUAGES"
            }
            else -> "PENDING"
        }
        // Partial failure summary: count of failed languages for enterprise visibility
        val failedCount = supportedLangs.count { lang ->
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
        return allStoryIds.distinct().mapNotNull { masterId ->
            val story = repository.findById(masterId) ?: return@mapNotNull null
            val translationIssues = byMaster[masterId]?.map { t ->
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
            if (masterId == 33L || masterId == 36L) {
                // #region agent log
                debugLog(
                    runId = "triage-visibility",
                    hypothesisId = "H16",
                    location = "StoryLibraryService.kt:getStoriesWithIssues:decision",
                    message = "triage inclusion decision",
                    data = mapOf(
                        "storyId" to masterId,
                        "activeLang" to (activeStories[masterId] ?: ""),
                        "issuesCount" to issues.size
                    )
                )
                // #endregion
            }
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
     * Mark one or more languages as reviewed for a story.
     * Idempotent: already-reviewed are left as-is.
     * Enables Approve when all pipeline languages are reviewed (admin uses "Have reviewed" in View modal).
     */
    @Transactional
    fun markLanguagesReviewed(masterStoryId: Long, languages: List<String>) {
        if (languages.isEmpty()) return
        val existing = libraryStoryLanguageReviewJpaRepository.findByLibraryStoryId(masterStoryId)
            .map { it.language }.toSet()
        val toAdd = languages.map { it.trim().lowercase() }.filter { it.isNotBlank() && it !in existing }
        for (lang in toAdd) {
            libraryStoryLanguageReviewJpaRepository.save(
                LibraryStoryLanguageReviewEntity(libraryStoryId = masterStoryId, language = lang)
            )
        }
        if (toAdd.isNotEmpty()) {
            log.info("Marked languages as reviewed storyId={} languages={}", masterStoryId, toAdd)
        }
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
