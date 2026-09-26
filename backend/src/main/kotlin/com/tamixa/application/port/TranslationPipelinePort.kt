package com.tamixa.application.port

import com.tamixa.domain.TranslationPipelineStatus

/**
 * Port for translation pipeline operations.
 * Defines the contract for parallel translation processing.
 */
interface TranslationPipelinePort {
    
    /**
     * Triggers the translation pipeline for a story.
     * Processes all configured languages in parallel.
     * 
     * @param storyId The library story ID to process
     * @return PipelineExecutionResult with status for each language
     */
    suspend fun triggerPipeline(storyId: Long): PipelineExecutionResult
    
    /**
     * Gets the current pipeline status for a story across all languages.
     * Used by admin dashboard for status polling.
     * 
     * @param storyId The library story ID
     * @return Map of language to translation status
     */
    fun getPipelineStatus(storyId: Long): Map<String, TranslationPipelineStatus>
    
    /**
     * Retries failed translations for a story.
     * Only retries translations that haven't exceeded max retry count.
     * 
     * @param storyId The library story ID
     * @return List of languages that were retried
     */
    suspend fun retryFailedTranslations(storyId: Long): List<String>
}

/**
 * Data class representing the result of a single language translation.
 */
data class LanguageTranslationResult(
    val language: String,
    val success: Boolean,
    val error: String? = null,
    val translationId: Long? = null
)

/**
 * Data class representing the overall pipeline execution result.
 */
data class PipelineExecutionResult(
    val storyId: Long,
    val success: Boolean,
    val languageResults: List<LanguageTranslationResult>,
    val totalDurationMs: Long
)
