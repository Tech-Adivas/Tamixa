package com.tamixa.api.library

import com.tamixa.api.library.dto.PipelineStatusResponse
import com.tamixa.api.library.dto.TriggerPipelineRequest
import com.tamixa.api.library.dto.TriggerPipelineResponse
import com.tamixa.application.port.TranslationPipelinePort
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * REST controller for translation pipeline operations.
 * Provides endpoints for triggering pipeline, checking status, and retrying failures.
 * 
 * Endpoints:
 * - POST /api/v1/library/stories/{id}/pipeline/trigger - Trigger pipeline for a story
 * - GET /api/v1/library/stories/{id}/pipeline/status - Get pipeline status for all languages
 * - POST /api/v1/library/stories/{id}/pipeline/retry - Retry failed translations
 * 
 * Security:
 * - All endpoints require ADMIN or CONTENT_MANAGER role
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/requirements.md Requirement 3
 */
@RestController
@RequestMapping("/api/v1/library/stories")
class TranslationPipelineController(
    private val translationPipeline: TranslationPipelinePort
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Triggers the translation pipeline for a story.
     * Processes all configured languages in parallel.
     * 
     * @param id The library story ID
     * @param request Optional request body (reserved for future parameters)
     * @return Pipeline execution result with status for each language
     */
    @PostMapping("/{id}/pipeline/trigger")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun triggerPipeline(
        @PathVariable id: Long,
        @RequestBody(required = false) request: TriggerPipelineRequest?
    ): ResponseEntity<TriggerPipelineResponse> {
        log.info("Triggering pipeline for storyId={}", id)
        
        return try {
            val result = runBlocking {
                translationPipeline.triggerPipeline(id)
            }
            
            val response = TriggerPipelineResponse(
                storyId = result.storyId,
                success = result.success,
                message = if (result.success) {
                    "Pipeline triggered successfully for all languages"
                } else {
                    "Pipeline completed with some failures"
                },
                languageResults = result.languageResults.map { lr ->
                    TriggerPipelineResponse.LanguageResult(
                        language = lr.language,
                        success = lr.success,
                        error = lr.error,
                        translationId = lr.translationId
                    )
                },
                totalDurationMs = result.totalDurationMs
            )
            
            if (result.success) {
                ResponseEntity.ok(response)
            } else {
                ResponseEntity.status(207).body(response) // 207 Multi-Status for partial success
            }
        } catch (e: IllegalArgumentException) {
            log.error("Invalid request for storyId={}", id, e)
            ResponseEntity.badRequest().body(
                TriggerPipelineResponse(
                    storyId = id,
                    success = false,
                    message = e.message ?: "Invalid request",
                    languageResults = emptyList(),
                    totalDurationMs = 0
                )
            )
        } catch (e: IllegalStateException) {
            log.error("Invalid state for storyId={}", id, e)
            ResponseEntity.status(409).body(
                TriggerPipelineResponse(
                    storyId = id,
                    success = false,
                    message = e.message ?: "Invalid state",
                    languageResults = emptyList(),
                    totalDurationMs = 0
                )
            )
        } catch (e: Exception) {
            log.error("Pipeline trigger failed for storyId={}", id, e)
            ResponseEntity.internalServerError().body(
                TriggerPipelineResponse(
                    storyId = id,
                    success = false,
                    message = "Pipeline execution failed: ${e.message}",
                    languageResults = emptyList(),
                    totalDurationMs = 0
                )
            )
        }
    }
    
    /**
     * Gets the current pipeline status for a story across all languages.
     * Used by admin dashboard for status polling (every 3 seconds).
     * 
     * @param id The library story ID
     * @return Map of language to translation status
     */
    @GetMapping("/{id}/pipeline/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun getPipelineStatus(@PathVariable id: Long): ResponseEntity<PipelineStatusResponse> {
        log.debug("Getting pipeline status for storyId={}", id)
        
        return try {
            val statusMap = translationPipeline.getPipelineStatus(id)
            
            val response = PipelineStatusResponse(
                storyId = id,
                languageStatuses = statusMap.map { (lang, status) ->
                    PipelineStatusResponse.LanguageStatus(
                        language = lang,
                        status = status.name,
                        displayName = status.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                        isFailed = status.isFailed(),
                        isTerminal = status.isTerminal()
                    )
                }
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            log.error("Failed to get pipeline status for storyId={}", id, e)
            ResponseEntity.internalServerError().body(
                PipelineStatusResponse(
                    storyId = id,
                    languageStatuses = emptyList()
                )
            )
        }
    }
    
    /**
     * Retries failed translations for a story.
     * Only retries translations that haven't exceeded max retry count.
     * 
     * @param id The library story ID
     * @return List of languages that were retried
     */
    @PostMapping("/{id}/pipeline/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun retryFailedTranslations(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        log.info("Retrying failed translations for storyId={}", id)
        
        return try {
            val retriedLanguages = runBlocking {
                translationPipeline.retryFailedTranslations(id)
            }
            
            val response = mapOf(
                "storyId" to id,
                "success" to true,
                "message" to if (retriedLanguages.isEmpty()) {
                    "No failed translations to retry"
                } else {
                    "Retried ${retriedLanguages.size} language(s)"
                },
                "retriedLanguages" to retriedLanguages
            )
            
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            log.error("Invalid request for storyId={}", id, e)
            ResponseEntity.badRequest().body(
                mapOf(
                    "storyId" to id,
                    "success" to false,
                    "message" to (e.message ?: "Invalid request"),
                    "retriedLanguages" to emptyList<String>()
                )
            )
        } catch (e: Exception) {
            log.error("Retry failed for storyId={}", id, e)
            ResponseEntity.internalServerError().body(
                mapOf(
                    "storyId" to id,
                    "success" to false,
                    "message" to "Retry failed: ${e.message}",
                    "retriedLanguages" to emptyList<String>()
                )
            )
        }
    }
}
