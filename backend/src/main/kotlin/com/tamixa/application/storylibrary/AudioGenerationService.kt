package com.tamixa.application.storylibrary

import com.tamixa.api.library.dto.AudioGenerationResponse
import com.tamixa.api.library.dto.AudioStatusResponse
import com.tamixa.application.narration.StoryProcessingOrchestrator
import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.domain.LibraryStoryStatus
import com.tamixa.domain.narration.NarrationAudioStatus
import com.tamixa.infrastructure.config.AppProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service for managing audio generation (TTS) operations for library stories.
 * 
 * Responsibilities:
 * - Trigger audio generation for all languages or specific language
 * - Check audio generation status across all languages
 * - Retry failed audio generations
 * - Validate story status before audio generation
 * 
 * Architecture:
 * - Uses StoryProcessingOrchestrator for actual TTS generation
 * - Runs TTS operations asynchronously using @Async
 * - Supports parallel processing for multiple languages
 * - Integrates with existing narration infrastructure
 * 
 * Business Rules:
 * - Audio generation only works for stories in APPROVED status
 * - Each language has independent audio generation status
 * - Failed audio can be retried up to max retry count
 * - Supports 6 languages: ta, en, hi, te, kn, ml
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/requirements.md Requirement 12
 */
@Service
class AudioGenerationService(
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val storyTranslationRepository: StoryTranslationRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val storyProcessingOrchestrator: StoryProcessingOrchestrator,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    private val sourceLanguage: String
        get() = appProperties.translationPipeline.sourceLanguage.trim().lowercase()
    
    private val supportedLanguages: List<String>
        get() = (listOf(sourceLanguage) + appProperties.translationPipeline.targetLanguages
            .split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }).distinct()
    
    /**
     * Generate audio for all configured languages.
     * Runs asynchronously in background.
     * 
     * @param storyId Library story ID
     * @param voiceProfile Voice profile to use (default: "default")
     * @param forceRegenerate Force regeneration even if audio exists
     * @return Response with generation status for each language
     * @throws IllegalStateException if story is not in APPROVED status
     */
    fun generateAudioForAllLanguages(
        storyId: Long,
        voiceProfile: String = "default",
        forceRegenerate: Boolean = false
    ): AudioGenerationResponse {
        val story = storyLibraryRepository.findById(storyId)
            ?: throw IllegalArgumentException("Story not found: $storyId")
        
        // Validate story status
        if (story.status != LibraryStoryStatus.APPROVED) {
            throw IllegalStateException(
                "Cannot generate audio for story in ${story.status} status. Story must be in APPROVED status."
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        // Get all translations for this story
        val translations = storyTranslationRepository.findByMasterStoryId(storyId)
        val languagesToProcess = supportedLanguages.filter { lang ->
            translations.any { it.language == lang }
        }
        
        if (languagesToProcess.isEmpty()) {
            return AudioGenerationResponse(
                storyId = storyId,
                success = false,
                message = "No translations found for configured languages",
                languageResults = emptyList(),
                totalDurationMs = System.currentTimeMillis() - startTime
            )
        }
        
        log.info("Starting audio generation for storyId={} languages={} voiceProfile={} forceRegenerate={}",
            storyId, languagesToProcess, voiceProfile, forceRegenerate)
        
        // Clear existing audio if force regenerate
        if (forceRegenerate) {
            translations.forEach { translation ->
                narrationAudioRepository.deleteByTranslationIdAndVoiceProfile(translation.id, voiceProfile)
            }
        }
        
        // Trigger async audio generation
        triggerAsyncAudioGeneration(storyId, languagesToProcess, voiceProfile)
        
        // Update story status to AUDIO_GENERATING
        storyLibraryRepository.updateStatus(storyId, LibraryStoryStatus.AUDIO_GENERATING)
        
        val languageResults = languagesToProcess.map { lang ->
            AudioGenerationResponse.LanguageAudioResult(
                language = lang,
                success = true,
                status = "PENDING",
                error = null
            )
        }
        
        return AudioGenerationResponse(
            storyId = storyId,
            success = true,
            message = "Audio generation started for ${languagesToProcess.size} language(s)",
            languageResults = languageResults,
            totalDurationMs = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * Generate audio for a specific language.
     * Runs asynchronously in background.
     * 
     * @param storyId Library story ID
     * @param language Language code (ta, en, hi, te, kn, ml)
     * @param voiceProfile Voice profile to use (default: "default")
     * @param forceRegenerate Force regeneration even if audio exists
     * @return Response with generation status
     * @throws IllegalArgumentException if language is not supported
     * @throws IllegalStateException if story is not in APPROVED status
     */
    fun generateAudioForLanguage(
        storyId: Long,
        language: String,
        voiceProfile: String = "default",
        forceRegenerate: Boolean = false
    ): AudioGenerationResponse {
        val normalizedLang = language.trim().lowercase()
        
        // Validate language
        if (normalizedLang !in supportedLanguages) {
            throw IllegalArgumentException(
                "Unsupported language: $language. Supported languages: ${supportedLanguages.joinToString(", ")}"
            )
        }
        
        val story = storyLibraryRepository.findById(storyId)
            ?: throw IllegalArgumentException("Story not found: $storyId")
        
        // Validate story status
        if (story.status != LibraryStoryStatus.APPROVED && story.status != LibraryStoryStatus.AUDIO_GENERATING) {
            throw IllegalStateException(
                "Cannot generate audio for story in ${story.status} status. Story must be in APPROVED or AUDIO_GENERATING status."
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        // Get translation for this language
        val translation = storyTranslationRepository.findByMasterStoryIdAndLanguage(storyId, normalizedLang)
            ?: throw IllegalArgumentException("Translation not found for language: $language")
        
        log.info("Starting audio generation for storyId={} language={} voiceProfile={} forceRegenerate={}",
            storyId, normalizedLang, voiceProfile, forceRegenerate)
        
        // Clear existing audio if force regenerate
        if (forceRegenerate) {
            narrationAudioRepository.deleteByTranslationIdAndVoiceProfile(translation.id, voiceProfile)
        }
        
        // Trigger async audio generation
        triggerAsyncAudioGeneration(storyId, listOf(normalizedLang), voiceProfile)
        
        // Update story status to AUDIO_GENERATING if not already
        if (story.status == LibraryStoryStatus.APPROVED) {
            storyLibraryRepository.updateStatus(storyId, LibraryStoryStatus.AUDIO_GENERATING)
        }
        
        val languageResult = AudioGenerationResponse.LanguageAudioResult(
            language = normalizedLang,
            success = true,
            status = "PENDING",
            error = null
        )
        
        return AudioGenerationResponse(
            storyId = storyId,
            success = true,
            message = "Audio generation started for language: $normalizedLang",
            languageResults = listOf(languageResult),
            totalDurationMs = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * Get audio generation status for all languages.
     * 
     * @param storyId Library story ID
     * @return Audio status for all configured languages
     * @throws IllegalArgumentException if story not found
     */
    fun getAudioStatus(storyId: Long): AudioStatusResponse {
        val story = storyLibraryRepository.findById(storyId)
            ?: throw IllegalArgumentException("Story not found: $storyId")
        
        val translations = storyTranslationRepository.findByMasterStoryId(storyId)
        
        val languageStatuses = supportedLanguages.mapNotNull { lang ->
            val translation = translations.find { it.language == lang } ?: return@mapNotNull null
            
            val narrationAudio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(
                translation.id,
                "default"
            )
            
            val status = narrationAudio?.status?.name ?: "NONE"
            val hasAudio = narrationAudio?.status == NarrationAudioStatus.READY
            val canRetry = narrationAudio?.status == NarrationAudioStatus.FAILED
            
            AudioStatusResponse.LanguageAudioStatus(
                language = lang,
                status = status,
                displayName = getLanguageDisplayName(lang),
                audioUrl = if (hasAudio) narrationAudio?.audioUrl else null,
                durationSeconds = if (hasAudio) narrationAudio?.durationSeconds else null,
                fileSizeBytes = null, // File size not stored in current schema
                generatedAt = if (hasAudio) narrationAudio?.createdAt else null,
                error = null, // Error details not stored in current schema
                hasAudio = hasAudio,
                canRetry = canRetry,
                truncationWarning = narrationAudio?.truncationWarning ?: false
            )
        }
        
        val overallStatus = AudioStatusResponse.determineOverallStatus(languageStatuses)
        
        // Update story status based on overall audio status
        updateStoryStatusBasedOnAudioStatus(storyId, story.status, overallStatus)
        
        return AudioStatusResponse(
            storyId = storyId,
            overallStatus = overallStatus,
            languageAudioStatuses = languageStatuses
        )
    }
    
    /**
     * Retry failed audio generations.
     * Only retries languages with FAILED status.
     * 
     * @param storyId Library story ID
     * @param voiceProfile Voice profile to use (default: "default")
     * @return Response with retry status for each language
     * @throws IllegalStateException if story is not in APPROVED or AUDIO_FAILED status
     */
    fun retryFailedAudio(
        storyId: Long,
        voiceProfile: String = "default"
    ): AudioGenerationResponse {
        val story = storyLibraryRepository.findById(storyId)
            ?: throw IllegalArgumentException("Story not found: $storyId")
        
        // Validate story status
        if (story.status != LibraryStoryStatus.APPROVED && 
            story.status != LibraryStoryStatus.AUDIO_FAILED &&
            story.status != LibraryStoryStatus.AUDIO_GENERATING) {
            throw IllegalStateException(
                "Cannot retry audio for story in ${story.status} status. Story must be in APPROVED, AUDIO_GENERATING, or AUDIO_FAILED status."
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        val translations = storyTranslationRepository.findByMasterStoryId(storyId)
        
        // Find languages with failed audio
        val failedLanguages = supportedLanguages.filter { lang ->
            val translation = translations.find { it.language == lang } ?: return@filter false
            val narrationAudio = narrationAudioRepository.findByTranslationIdAndVoiceProfile(
                translation.id,
                voiceProfile
            )
            narrationAudio?.status == NarrationAudioStatus.FAILED
        }
        
        if (failedLanguages.isEmpty()) {
            return AudioGenerationResponse(
                storyId = storyId,
                success = true,
                message = "No failed audio to retry",
                languageResults = emptyList(),
                totalDurationMs = System.currentTimeMillis() - startTime
            )
        }
        
        log.info("Retrying failed audio for storyId={} languages={} voiceProfile={}",
            storyId, failedLanguages, voiceProfile)
        
        // Clear failed audio records
        failedLanguages.forEach { lang ->
            val translation = translations.find { it.language == lang }
            if (translation != null) {
                narrationAudioRepository.deleteByTranslationIdAndVoiceProfile(translation.id, voiceProfile)
            }
        }
        
        // Trigger async audio generation
        triggerAsyncAudioGeneration(storyId, failedLanguages, voiceProfile)
        
        // Update story status to AUDIO_GENERATING
        storyLibraryRepository.updateStatus(storyId, LibraryStoryStatus.AUDIO_GENERATING)
        
        val languageResults = failedLanguages.map { lang ->
            AudioGenerationResponse.LanguageAudioResult(
                language = lang,
                success = true,
                status = "PENDING",
                error = null
            )
        }
        
        return AudioGenerationResponse(
            storyId = storyId,
            success = true,
            message = "Audio retry started for ${failedLanguages.size} language(s)",
            languageResults = languageResults,
            totalDurationMs = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * Trigger async audio generation for specified languages.
     * Uses @Async to run in background thread pool.
     */
    @Async
    fun triggerAsyncAudioGeneration(
        storyId: Long,
        languages: List<String>,
        voiceProfile: String
    ) {
        log.info("Async audio generation started: storyId={} languages={} voiceProfile={}",
            storyId, languages, voiceProfile)
        
        try {
            val translations = storyTranslationRepository.findByMasterStoryId(storyId)
            
            // Process languages in parallel using coroutines
            runBlocking {
                languages.map { lang ->
                    async {
                        try {
                            val translation = translations.find { it.language == lang }
                            if (translation != null) {
                                log.debug("Processing audio for storyId={} language={}", storyId, lang)
                                storyProcessingOrchestrator.process(
                                    translation = translation,
                                    voiceProfiles = listOf(voiceProfile)
                                )
                                log.info("Audio generation completed: storyId={} language={}", storyId, lang)
                            } else {
                                log.warn("Translation not found for storyId={} language={}", storyId, lang)
                            }
                        } catch (e: Exception) {
                            log.error("Audio generation failed for storyId={} language={}", storyId, lang, e)
                        }
                    }
                }.awaitAll()
            }
            
            log.info("Async audio generation completed: storyId={} languages={}", storyId, languages)
            
            // Check if all audio is ready and update story status
            val audioStatus = getAudioStatus(storyId)
            updateStoryStatusBasedOnAudioStatus(storyId, null, audioStatus.overallStatus)
            
        } catch (e: Exception) {
            log.error("Async audio generation failed for storyId={}", storyId, e)
            
            // Update story status to AUDIO_FAILED
            try {
                storyLibraryRepository.updateStatus(storyId, LibraryStoryStatus.AUDIO_FAILED)
            } catch (statusUpdateError: Exception) {
                log.error("Failed to update story status to AUDIO_FAILED for storyId={}", storyId, statusUpdateError)
            }
        }
    }
    
    /**
     * Update story status based on overall audio status.
     */
    private fun updateStoryStatusBasedOnAudioStatus(
        storyId: Long,
        currentStatus: LibraryStoryStatus?,
        overallAudioStatus: String
    ) {
        val newStatus = when (overallAudioStatus) {
            "ALL_READY" -> LibraryStoryStatus.AUDIO_REVIEW
            "FAILED" -> LibraryStoryStatus.AUDIO_FAILED
            "GENERATING", "PARTIAL" -> LibraryStoryStatus.AUDIO_GENERATING
            else -> return // Don't update status for NONE
        }
        
        // Only update if status has changed
        if (currentStatus != null && currentStatus == newStatus) {
            return
        }
        
        try {
            storyLibraryRepository.updateStatus(storyId, newStatus)
            log.info("Updated story status: storyId={} status={} (audio: {})", 
                storyId, newStatus, overallAudioStatus)
        } catch (e: Exception) {
            log.error("Failed to update story status for storyId={}", storyId, e)
        }
    }
    
    /**
     * Get display name for language code.
     */
    private fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "ta" -> "Tamil"
            "en" -> "English"
            "hi" -> "Hindi"
            "te" -> "Telugu"
            "kn" -> "Kannada"
            "ml" -> "Malayalam"
            else -> languageCode.uppercase()
        }
    }
}
