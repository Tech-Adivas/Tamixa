package com.tamixa.application.pipeline

import com.tamixa.application.port.*
import com.tamixa.application.translation.TranslationService
import com.tamixa.domain.LibraryStoryStatus
import com.tamixa.domain.StoryTranslation
import com.tamixa.domain.TranslationPipelineStatus
import com.tamixa.infrastructure.config.AppProperties
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Parallel translation pipeline service that processes story translations for multiple languages concurrently.
 * 
 * Features:
 * - Processes translations for all configured languages (Tamil, English, Hindi, Telugu, Kannada, Malayalam) in parallel
 * - Updates translation status for each language independently
 * - Handles failures gracefully with retry logic and error tracking
 * - Integrates with LibraryStoryStatus enum system
 * - Provides status tracking for admin dashboard
 * 
 * Architecture:
 * - Uses Kotlin coroutines for parallel processing
 * - Each language translation runs in its own coroutine with timeout
 * - Failures in one language don't block others
 * - Atomic status updates per language
 * 
 * Workflow:
 * 1. Story submitted (DRAFT → SUBMITTED)
 * 2. Pipeline triggered (SUBMITTED → TRANSLATING)
 * 3. All languages processed in parallel
 * 4. On success: TRANSLATING → CONTENT_REVIEW
 * 5. On failure: TRANSLATING → TRANSLATION_FAILED
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/requirements.md Requirement 3
 * See: .kiro/specs/tamixa-premium-ux-overhaul/design.md Task 7
 */
@Service
class ParallelTranslationPipelineService(
    private val translationService: TranslationService,
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val translationRepository: StoryTranslationRepositoryPort,
    private val appProperties: AppProperties
) : TranslationPipelinePort {
    private val log = LoggerFactory.getLogger(javaClass)
    
    private val sourceLanguage get() = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
    private val targetLanguages get() = appProperties.translationPipeline.targetLanguages
        .split(",")
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
    private val languageTimeoutMinutes get() = appProperties.translationPipeline.languageTimeoutMinutes
    private val totalTimeoutMinutes get() = appProperties.translationPipeline.totalTimeoutMinutes
    private val maxRetries get() = appProperties.translationPipeline.maxRetries
    
    /**
     * Triggers the translation pipeline for a story.
     * Processes all configured languages in parallel.
     * 
     * @param storyId The library story ID to process
     * @return PipelineExecutionResult with status for each language
     */
    override suspend fun triggerPipeline(storyId: Long): PipelineExecutionResult {
        val startTime = System.currentTimeMillis()
        
        log.info("Starting parallel translation pipeline for storyId={}", storyId)
        
        // Load story
        val story = storyLibraryRepository.findById(storyId)
        if (story == null) {
            log.error("Story not found: storyId={}", storyId)
            throw IllegalArgumentException("Story not found: $storyId")
        }
        
        // Validate story status
        if (!story.status.canTransitionTo(LibraryStoryStatus.TRANSLATING)) {
            log.error("Invalid status transition for storyId={}: current={}", storyId, story.status)
            throw IllegalStateException("Cannot start pipeline from status: ${story.status}")
        }
        
        // Update story status to TRANSLATING
        storyLibraryRepository.updateStatus(storyId, LibraryStoryStatus.TRANSLATING)
        log.info("Updated story status to TRANSLATING: storyId={}", storyId)
        
        // Get all target languages (including source if configured)
        val allLanguages = (listOf(sourceLanguage) + targetLanguages).distinct()
        log.info("Processing {} languages for storyId={}: {}", allLanguages.size, storyId, allLanguages)
        
        // Process all languages in parallel with timeout
        val languageResults = try {
            withTimeout(totalTimeoutMinutes * 60 * 1000L) {
                processLanguagesInParallel(story.id, story, allLanguages)
            }
        } catch (e: TimeoutCancellationException) {
            log.error("Pipeline timeout exceeded for storyId={}: {}min", storyId, totalTimeoutMinutes, e)
            handlePipelineTimeout(storyId, allLanguages)
        } catch (e: Exception) {
            log.error("Pipeline execution failed for storyId={}", storyId, e)
            handlePipelineFailure(storyId, allLanguages, e)
        }
        
        // Determine overall success
        val allSuccessful = languageResults.all { it.success }
        val anySuccessful = languageResults.any { it.success }
        
        // Update story status based on results
        val finalStatus = when {
            allSuccessful -> {
                log.info("All languages completed successfully for storyId={}", storyId)
                LibraryStoryStatus.CONTENT_REVIEW
            }
            anySuccessful -> {
                log.warn("Partial success for storyId={}: {} of {} languages succeeded", 
                    storyId, languageResults.count { it.success }, languageResults.size)
                LibraryStoryStatus.TRANSLATION_FAILED
            }
            else -> {
                log.error("All languages failed for storyId={}", storyId)
                LibraryStoryStatus.TRANSLATION_FAILED
            }
        }
        
        storyLibraryRepository.updateStatus(storyId, finalStatus)
        log.info("Updated story status to {} for storyId={}", finalStatus, storyId)
        
        val duration = System.currentTimeMillis() - startTime
        log.info("Pipeline completed for storyId={} in {}ms: status={}, results={}", 
            storyId, duration, finalStatus, languageResults)
        
        return PipelineExecutionResult(
            storyId = storyId,
            success = allSuccessful,
            languageResults = languageResults,
            totalDurationMs = duration
        )
    }
    
    /**
     * Processes all languages in parallel using coroutines.
     * Each language runs in its own coroutine with independent timeout and error handling.
     */
    private suspend fun processLanguagesInParallel(
        storyId: Long,
        story: com.tamixa.domain.LibraryStory,
        languages: List<String>
    ): List<LanguageTranslationResult> = coroutineScope {
        languages.map { language ->
            async {
                processLanguageWithTimeout(storyId, story, language)
            }
        }.awaitAll()
    }
    
    /**
     * Processes a single language with timeout and error handling.
     */
    private suspend fun processLanguageWithTimeout(
        storyId: Long,
        story: com.tamixa.domain.LibraryStory,
        language: String
    ): LanguageTranslationResult {
        return try {
            withTimeout(languageTimeoutMinutes * 60 * 1000L) {
                processLanguage(storyId, story, language)
            }
        } catch (e: TimeoutCancellationException) {
            log.error("Language timeout for storyId={}, language={}: {}min", 
                storyId, language, languageTimeoutMinutes, e)
            handleLanguageTimeout(storyId, language)
        } catch (e: Exception) {
            log.error("Language processing failed for storyId={}, language={}", 
                storyId, language, e)
            handleLanguageFailure(storyId, language, e)
        }
    }
    
    /**
     * Processes translation for a single language.
     * Creates or updates the translation record with translated content.
     */
    private suspend fun processLanguage(
        storyId: Long,
        story: com.tamixa.domain.LibraryStory,
        language: String
    ): LanguageTranslationResult = withContext(Dispatchers.IO) {
        log.info("Processing language={} for storyId={}", language, storyId)
        
        // Find or create translation record
        var translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, language)
        
        if (translation == null) {
            // Create new translation record
            translation = StoryTranslation(
                id = 0, // Will be assigned by repository
                masterStoryId = storyId,
                language = language,
                title = story.title,
                content = story.content,
                moral = story.moral,
                wordCount = story.wordCount,
                readingTimeMinutes = story.readingTimeMinutes,
                createdAt = Instant.now(),
                status = TranslationPipelineStatus.TRANSLATING,
                retryCount = 0,
                lastError = null,
                narrationApprovedAt = null,
                interactiveGraphJson = story.interactiveGraphJson,
                postStoryMission = story.postStoryMission,
                postStoryResourceUrl = story.postStoryResourceUrl,
                parentContentNote = story.parentContentNote,
                speakAlongPrompt = story.speakAlongPrompt,
                parentDiscussionPrompts = story.parentDiscussionPrompts
            )
            translation = translationRepository.save(translation)
            log.info("Created translation record: id={}, storyId={}, language={}", 
                translation.id, storyId, language)
        } else {
            // Update existing translation to TRANSLATING
            translationRepository.atomicStatusUpdate(
                translation.id,
                TranslationPipelineStatus.TRANSLATING,
                null
            )
            log.info("Updated translation to TRANSLATING: id={}, storyId={}, language={}", 
                translation.id, storyId, language)
        }
        
        try {
            // Perform translation
            val result = translationService.translateIfNeeded(
                sourceLang = sourceLanguage,
                targetLang = language,
                title = story.title,
                content = story.content,
                moral = story.moral,
                forceParaphraseForSameLanguage = false,
                bypassCache = false,
                parentContentNote = story.parentContentNote,
                parentDiscussionPrompts = story.parentDiscussionPrompts,
                speakAlongPrompt = story.speakAlongPrompt
            )
            
            // Update translation with results
            val updatedTranslation = translation.copy(
                title = result.title,
                content = result.content,
                moral = result.moral,
                wordCount = result.content.split(Regex("\\s+")).filter { it.isNotBlank() }.size,
                readingTimeMinutes = calculateReadingTime(result.content),
                status = TranslationPipelineStatus.AWAITING_AUDIO,
                lastError = null,
                parentContentNote = result.parentContentNote,
                parentDiscussionPrompts = result.parentDiscussionPrompts,
                speakAlongPrompt = result.speakAlongPrompt
            )
            
            translationRepository.save(updatedTranslation)
            
            log.info("Translation completed successfully: id={}, storyId={}, language={}, translated={}", 
                translation.id, storyId, language, result.translated)
            
            LanguageTranslationResult(
                language = language,
                success = true,
                translationId = translation.id
            )
        } catch (e: Exception) {
            log.error("Translation failed: id={}, storyId={}, language={}", 
                translation.id, storyId, language, e)
            
            // Update translation with error
            val shouldRetry = translation.retryCount < maxRetries
            if (shouldRetry) {
                translationRepository.incrementRetryCount(translation.id, e.message ?: "Unknown error")
                log.info("Incremented retry count for translation: id={}, retryCount={}", 
                    translation.id, translation.retryCount + 1)
            } else {
                translationRepository.atomicStatusUpdate(
                    translation.id,
                    TranslationPipelineStatus.TRANSLATION_FAILED,
                    e.message ?: "Unknown error"
                )
                log.error("Max retries exceeded for translation: id={}, maxRetries={}", 
                    translation.id, maxRetries)
            }
            
            LanguageTranslationResult(
                language = language,
                success = false,
                error = e.message ?: "Unknown error",
                translationId = translation.id
            )
        }
    }
    
    /**
     * Handles timeout for a single language.
     */
    private fun handleLanguageTimeout(storyId: Long, language: String): LanguageTranslationResult {
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, language)
        if (translation != null) {
            val errorMsg = "Language processing timeout: ${languageTimeoutMinutes}min"
            translationRepository.atomicStatusUpdate(
                translation.id,
                TranslationPipelineStatus.TRANSLATION_FAILED,
                errorMsg
            )
        }
        return LanguageTranslationResult(
            language = language,
            success = false,
            error = "Timeout: ${languageTimeoutMinutes}min"
        )
    }
    
    /**
     * Handles failure for a single language.
     */
    private fun handleLanguageFailure(
        storyId: Long,
        language: String,
        error: Exception
    ): LanguageTranslationResult {
        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, language)
        if (translation != null) {
            translationRepository.atomicStatusUpdate(
                translation.id,
                TranslationPipelineStatus.TRANSLATION_FAILED,
                error.message ?: "Unknown error"
            )
        }
        return LanguageTranslationResult(
            language = language,
            success = false,
            error = error.message ?: "Unknown error"
        )
    }
    
    /**
     * Handles timeout for the entire pipeline.
     */
    private fun handlePipelineTimeout(storyId: Long, languages: List<String>): List<LanguageTranslationResult> {
        log.error("Pipeline timeout for storyId={}, marking all languages as failed", storyId)
        return languages.map { language ->
            handleLanguageTimeout(storyId, language)
        }
    }
    
    /**
     * Handles failure for the entire pipeline.
     */
    private fun handlePipelineFailure(
        storyId: Long,
        languages: List<String>,
        error: Exception
    ): List<LanguageTranslationResult> {
        log.error("Pipeline failure for storyId={}, marking all languages as failed", storyId, error)
        return languages.map { language ->
            handleLanguageFailure(storyId, language, error)
        }
    }
    
    /**
     * Calculates reading time in minutes based on word count.
     * Assumes average reading speed of 200 words per minute.
     */
    private fun calculateReadingTime(content: String): Double {
        val wordCount = content.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        return wordCount / 200.0
    }
    
    /**
     * Gets the current pipeline status for a story across all languages.
     * Used by admin dashboard for status polling.
     * 
     * @param storyId The library story ID
     * @return Map of language to translation status
     */
    override fun getPipelineStatus(storyId: Long): Map<String, TranslationPipelineStatus> {
        val translations = translationRepository.findByMasterStoryId(storyId)
        return translations.associate { it.language to it.status }
    }
    
    /**
     * Retries failed translations for a story.
     * Only retries translations that haven't exceeded max retry count.
     * 
     * @param storyId The library story ID
     * @return List of languages that were retried
     */
    override suspend fun retryFailedTranslations(storyId: Long): List<String> {
        log.info("Retrying failed translations for storyId={}", storyId)
        
        val story = storyLibraryRepository.findById(storyId)
        if (story == null) {
            log.error("Story not found: storyId={}", storyId)
            throw IllegalArgumentException("Story not found: $storyId")
        }
        
        val translations = translationRepository.findByMasterStoryId(storyId)
        val failedTranslations = translations.filter { 
            it.status.isFailed() && it.retryCount < maxRetries 
        }
        
        if (failedTranslations.isEmpty()) {
            log.info("No failed translations to retry for storyId={}", storyId)
            return emptyList()
        }
        
        log.info("Retrying {} failed translations for storyId={}", failedTranslations.size, storyId)
        
        val retriedLanguages = failedTranslations.map { it.language }
        
        // Process failed languages in parallel
        coroutineScope {
            failedTranslations.map { translation ->
                async {
                    processLanguageWithTimeout(storyId, story, translation.language)
                }
            }.awaitAll()
        }
        
        return retriedLanguages
    }
}
