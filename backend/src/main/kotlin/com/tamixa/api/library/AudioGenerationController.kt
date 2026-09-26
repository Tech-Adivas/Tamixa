package com.tamixa.api.library

import com.tamixa.api.ApiVersion
import com.tamixa.api.library.dto.AudioGenerationRequest
import com.tamixa.api.library.dto.AudioGenerationResponse
import com.tamixa.api.library.dto.AudioStatusResponse
import com.tamixa.application.admin.AdminService
import com.tamixa.application.storylibrary.AudioGenerationService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * REST controller for audio generation (TTS) operations.
 * 
 * Provides endpoints for the audio generation workflow:
 * - Generate audio for all languages
 * - Generate audio for specific language
 * - Get audio generation status
 * - Retry failed audio generations
 * 
 * Security:
 * - All endpoints require ADMIN or CONTENT_MANAGER role
 * - Operations are logged in admin audit trail
 * 
 * Architecture:
 * - Follows hexagonal architecture (controller → service → port → adapter)
 * - Uses DTOs for request/response bodies
 * - Structured logging with context (storyId, language, status)
 * - Uses @Async for long-running TTS operations
 * - Integrates with existing TTS client adapter (GoogleCloudTtsClientAdapter or similar)
 * 
 * Workflow:
 * - Audio generation only works for stories in APPROVED status
 * - Supports 6 languages: Tamil (ta), English (en), Hindi (hi), Telugu (te), Kannada (kn), Malayalam (ml)
 * - Each language has independent audio generation status
 * - Parallel processing for multiple languages
 * - Admin dashboard polls status every 3 seconds
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/requirements.md Requirement 12
 * See: .kiro/specs/tamixa-premium-ux-overhaul/design.md Task 9
 */
@RestController
@RequestMapping("${ApiVersion.V1}/library/stories")
class AudioGenerationController(
    private val audioGenerationService: AudioGenerationService,
    private val adminService: AdminService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Generate audio for all configured languages.
     * 
     * Triggers TTS generation for all languages configured in the translation pipeline.
     * Only works for stories in APPROVED status.
     * Runs asynchronously in background - returns immediately with 202 Accepted.
     * 
     * Valid only when story is in APPROVED status.
     * 
     * @param id Story ID to generate audio for
     * @param request Optional request body with generation parameters
     * @return Accepted response indicating audio generation has started
     */
    @PostMapping("/{id}/audio/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun generateAudioForAllLanguages(
        @PathVariable id: Long,
        @Valid @RequestBody(required = false) request: AudioGenerationRequest?
    ): ResponseEntity<AudioGenerationResponse> {
        log.info("Generating audio for all languages: storyId={}", id)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        
        return try {
            val result = audioGenerationService.generateAudioForAllLanguages(
                storyId = id,
                voiceProfile = request?.voiceProfile ?: "default",
                forceRegenerate = request?.forceRegenerate ?: false
            )
            
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_generate_audio_all",
                "story",
                id.toString(),
                "Audio generation started for all languages"
            )
            
            log.info("Audio generation started: storyId={}, languages={}, admin={}", 
                id, result.languageResults.size, adminEmail)
            
            ResponseEntity.accepted().body(result)
        } catch (e: IllegalStateException) {
            log.warn("Cannot generate audio for storyId={}: {}", id, e.message)
            
            ResponseEntity.badRequest().body(
                AudioGenerationResponse(
                    storyId = id,
                    success = false,
                    message = e.message ?: "Cannot generate audio. Story must be in APPROVED status.",
                    languageResults = emptyList()
                )
            )
        } catch (e: Exception) {
            log.error("Audio generation failed for storyId={}", id, e)
            
            ResponseEntity.internalServerError().body(
                AudioGenerationResponse(
                    storyId = id,
                    success = false,
                    message = "Audio generation failed: ${e.message}",
                    languageResults = emptyList()
                )
            )
        }
    }
    
    /**
     * Generate audio for a specific language.
     * 
     * Triggers TTS generation for a single language.
     * Only works for stories in APPROVED status.
     * Runs asynchronously in background - returns immediately with 202 Accepted.
     * 
     * Valid only when story is in APPROVED status.
     * 
     * @param id Story ID to generate audio for
     * @param language Language code (ta, en, hi, te, kn, ml)
     * @param request Optional request body with generation parameters
     * @return Accepted response indicating audio generation has started
     */
    @PostMapping("/{id}/audio/generate/{language}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun generateAudioForLanguage(
        @PathVariable id: Long,
        @PathVariable language: String,
        @Valid @RequestBody(required = false) request: AudioGenerationRequest?
    ): ResponseEntity<AudioGenerationResponse> {
        log.info("Generating audio for language: storyId={}, language={}", id, language)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        
        return try {
            val result = audioGenerationService.generateAudioForLanguage(
                storyId = id,
                language = language.trim().lowercase(),
                voiceProfile = request?.voiceProfile ?: "default",
                forceRegenerate = request?.forceRegenerate ?: false
            )
            
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_generate_audio_language",
                "story",
                id.toString(),
                "Audio generation started for language: $language"
            )
            
            log.info("Audio generation started: storyId={}, language={}, admin={}", 
                id, language, adminEmail)
            
            ResponseEntity.accepted().body(result)
        } catch (e: IllegalArgumentException) {
            log.warn("Invalid language for storyId={}: {}", id, e.message)
            
            ResponseEntity.badRequest().body(
                AudioGenerationResponse(
                    storyId = id,
                    success = false,
                    message = e.message ?: "Invalid language code",
                    languageResults = emptyList()
                )
            )
        } catch (e: IllegalStateException) {
            log.warn("Cannot generate audio for storyId={} language={}: {}", id, language, e.message)
            
            ResponseEntity.badRequest().body(
                AudioGenerationResponse(
                    storyId = id,
                    success = false,
                    message = e.message ?: "Cannot generate audio. Story must be in APPROVED status.",
                    languageResults = emptyList()
                )
            )
        } catch (e: Exception) {
            log.error("Audio generation failed for storyId={} language={}", id, language, e)
            
            ResponseEntity.internalServerError().body(
                AudioGenerationResponse(
                    storyId = id,
                    success = false,
                    message = "Audio generation failed: ${e.message}",
                    languageResults = emptyList()
                )
            )
        }
    }
    
    /**
     * Get audio generation status for all languages.
     * 
     * Returns current audio status for each configured language.
     * Used by admin dashboard for polling (every 3 seconds).
     * 
     * Includes:
     * - Audio URL (if available)
     * - Duration in seconds
     * - File size in bytes
     * - Generation timestamp
     * - Error message (if failed)
     * 
     * @param id Story ID to check status for
     * @return Audio status for all languages
     */
    @GetMapping("/{id}/audio/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun getAudioStatus(@PathVariable id: Long): ResponseEntity<AudioStatusResponse> {
        log.debug("Getting audio status: storyId={}", id)
        
        return try {
            val status = audioGenerationService.getAudioStatus(id)
            ResponseEntity.ok(status)
        } catch (e: IllegalArgumentException) {
            log.warn("Story not found: storyId={}", id)
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            log.error("Failed to get audio status for storyId={}", id, e)
            ResponseEntity.internalServerError().build()
        }
    }
    
    /**
     * Retry failed audio generations.
     * 
     * Retries audio generation for all languages that have FAILED status.
     * Only retries languages that haven't exceeded max retry count.
     * Runs asynchronously in background - returns immediately with 202 Accepted.
     * 
     * Valid only when story is in APPROVED status.
     * 
     * @param id Story ID to retry audio for
     * @param request Optional request body with retry parameters
     * @return Accepted response with list of languages being retried
     */
    @PostMapping("/{id}/audio/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun retryFailedAudio(
        @PathVariable id: Long,
        @Valid @RequestBody(required = false) request: AudioGenerationRequest?
    ): ResponseEntity<AudioGenerationResponse> {
        log.info("Retrying failed audio: storyId={}", id)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        
        return try {
            val result = audioGenerationService.retryFailedAudio(
                storyId = id,
                voiceProfile = request?.voiceProfile ?: "default"
            )
            
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_retry_audio",
                "story",
                id.toString(),
                "Audio retry started for ${result.languageResults.size} language(s)"
            )
            
            log.info("Audio retry started: storyId={}, languages={}, admin={}", 
                id, result.languageResults.size, adminEmail)
            
            ResponseEntity.accepted().body(result)
        } catch (e: IllegalStateException) {
            log.warn("Cannot retry audio for storyId={}: {}", id, e.message)
            
            ResponseEntity.badRequest().body(
                AudioGenerationResponse(
                    storyId = id,
                    success = false,
                    message = e.message ?: "Cannot retry audio. Story must be in APPROVED status.",
                    languageResults = emptyList()
                )
            )
        } catch (e: Exception) {
            log.error("Audio retry failed for storyId={}", id, e)
            
            ResponseEntity.internalServerError().body(
                AudioGenerationResponse(
                    storyId = id,
                    success = false,
                    message = "Audio retry failed: ${e.message}",
                    languageResults = emptyList()
                )
            )
        }
    }
}
