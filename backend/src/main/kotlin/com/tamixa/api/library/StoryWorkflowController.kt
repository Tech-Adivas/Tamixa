package com.tamixa.api.library

import com.tamixa.api.ApiVersion
import com.tamixa.api.library.dto.StoryWorkflowRequest
import com.tamixa.api.library.dto.StoryWorkflowResponse
import com.tamixa.application.admin.AdminService
import com.tamixa.application.storylibrary.StoryLibraryService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

/**
 * REST controller for story management workflow operations.
 * 
 * Provides endpoints for the story review and approval workflow:
 * - Submit for review (DRAFT → SUBMITTED → TRANSLATING → CONTENT_REVIEW)
 * - Approve content (CONTENT_REVIEW → APPROVED)
 * - Request changes (CONTENT_REVIEW → CHANGES_REQUESTED)
 * - Reject story (CONTENT_REVIEW → REJECTED)
 * - Publish story (AUDIO_REVIEW → PUBLISHED)
 * - Unpublish story (PUBLISHED → DRAFT)
 * 
 * Security:
 * - All endpoints require ADMIN or CONTENT_MANAGER role
 * - Operations are logged in admin audit trail
 * 
 * Architecture:
 * - Follows hexagonal architecture (controller → service → port → adapter)
 * - Uses DTOs for request/response bodies
 * - Structured logging with context (storyId, status transitions)
 * - Integrates with ParallelTranslationPipelineService for submit operation
 * 
 * See: .kiro/specs/tamixa-premium-ux-overhaul/requirements.md Requirement 11
 * See: .kiro/specs/tamixa-premium-ux-overhaul/design.md Task 8
 */
@RestController
@RequestMapping("${ApiVersion.V1}/library/stories")
class StoryWorkflowController(
    private val storyLibraryService: StoryLibraryService,
    private val adminService: AdminService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    
    /**
     * Submit story for review.
     * 
     * Transitions story from DRAFT to SUBMITTED and triggers translation pipeline.
     * Pipeline will process all configured languages in parallel and transition to CONTENT_REVIEW when complete.
     * 
     * Valid only when story is in DRAFT status.
     * 
     * @param id Story ID to submit
     * @return Success response with new status
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun submitForReview(@PathVariable id: Long): ResponseEntity<StoryWorkflowResponse> {
        log.info("Submitting story for review: storyId={}", id)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        
        return if (storyLibraryService.submitForReview(id)) {
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_submit_for_review",
                "story",
                id.toString(),
                "Story submitted for review, translation pipeline triggered"
            )
            
            log.info("Story submitted successfully: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.ok(
                StoryWorkflowResponse(
                    success = true,
                    message = "Story submitted for review. Translation pipeline is running in background (~5 min).",
                    status = "SUBMITTED",
                    storyId = id
                )
            )
        } else {
            log.warn("Failed to submit story: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.badRequest().body(
                StoryWorkflowResponse(
                    success = false,
                    message = "Cannot submit story. Story must be in DRAFT status and have content.",
                    storyId = id
                )
            )
        }
    }
    
    /**
     * Approve story content.
     * 
     * Transitions story from CONTENT_REVIEW to APPROVED.
     * After approval, story is ready for audio generation.
     * Admin must then use Narration → Generate audio to produce TTS.
     * 
     * Valid only when story is in CONTENT_REVIEW status and all languages are reviewed.
     * 
     * @param id Story ID to approve
     * @return Success response with new status
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun approveStory(@PathVariable id: Long): ResponseEntity<StoryWorkflowResponse> {
        log.info("Approving story content: storyId={}", id)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        
        return if (storyLibraryService.approveStoryContent(id)) {
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_approve_content",
                "story",
                id.toString(),
                "Story content approved, ready for audio generation"
            )
            
            log.info("Story content approved: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.ok(
                StoryWorkflowResponse(
                    success = true,
                    message = "Story content approved. Ready for audio generation.",
                    status = "APPROVED",
                    storyId = id
                )
            )
        } else {
            log.warn("Failed to approve story: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.badRequest().body(
                StoryWorkflowResponse(
                    success = false,
                    message = "Cannot approve story. Story must be in CONTENT_REVIEW status and all languages must be reviewed.",
                    storyId = id
                )
            )
        }
    }
    
    /**
     * Request changes to story.
     * 
     * Transitions story from CONTENT_REVIEW to CHANGES_REQUESTED.
     * Sends story back to author with feedback notes.
     * Author must edit content and resubmit (DRAFT → SUBMITTED).
     * 
     * Valid only when story is in CONTENT_REVIEW status.
     * 
     * @param id Story ID
     * @param request Optional request body with feedback notes
     * @return Success response with new status
     */
    @PostMapping("/{id}/request-changes")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun requestChanges(
        @PathVariable id: Long,
        @Valid @RequestBody(required = false) request: StoryWorkflowRequest?
    ): ResponseEntity<StoryWorkflowResponse> {
        log.info("Requesting changes for story: storyId={}", id)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        val notes = request?.notes?.trim()?.takeIf { it.isNotBlank() }
        
        return if (storyLibraryService.requestStoryChanges(id, notes)) {
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_request_changes",
                "story",
                id.toString(),
                notes?.take(2000) ?: "Changes requested (no notes provided)"
            )
            
            log.info("Changes requested for story: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.ok(
                StoryWorkflowResponse(
                    success = true,
                    message = "Story sent back for changes. Author can edit and resubmit.",
                    status = "CHANGES_REQUESTED",
                    storyId = id
                )
            )
        } else {
            log.warn("Failed to request changes: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.badRequest().body(
                StoryWorkflowResponse(
                    success = false,
                    message = "Cannot request changes. Story must be in CONTENT_REVIEW status.",
                    storyId = id
                )
            )
        }
    }
    
    /**
     * Reject story.
     * 
     * Transitions story from CONTENT_REVIEW to REJECTED.
     * Story remains in database but is marked as rejected.
     * Author can edit and resubmit if desired.
     * 
     * Valid only when story is in CONTENT_REVIEW status.
     * 
     * @param id Story ID
     * @param request Optional request body with rejection reason
     * @return Success response with new status
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun rejectStory(
        @PathVariable id: Long,
        @Valid @RequestBody(required = false) request: StoryWorkflowRequest?
    ): ResponseEntity<StoryWorkflowResponse> {
        log.info("Rejecting story: storyId={}", id)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        val notes = request?.notes?.trim()?.takeIf { it.isNotBlank() }
        
        return if (storyLibraryService.rejectStoryContent(id, notes)) {
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_reject_story",
                "story",
                id.toString(),
                notes?.take(2000) ?: "Story rejected (no reason provided)"
            )
            
            log.info("Story rejected: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.ok(
                StoryWorkflowResponse(
                    success = true,
                    message = "Story rejected. Story remains in database and can be edited and resubmitted.",
                    status = "REJECTED",
                    storyId = id
                )
            )
        } else {
            log.warn("Failed to reject story: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.badRequest().body(
                StoryWorkflowResponse(
                    success = false,
                    message = "Cannot reject story. Story must be in CONTENT_REVIEW status.",
                    storyId = id
                )
            )
        }
    }
    
    /**
     * Publish approved story.
     * 
     * Transitions story from AUDIO_REVIEW to PUBLISHED.
     * Makes story visible to users on the mobile app.
     * 
     * Valid only when story is in AUDIO_REVIEW status (audio generated and reviewed).
     * 
     * @param id Story ID to publish
     * @return Success response with new status
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun publishStory(@PathVariable id: Long): ResponseEntity<StoryWorkflowResponse> {
        log.info("Publishing story: storyId={}", id)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        
        return if (storyLibraryService.publishStory(id)) {
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_publish_story",
                "story",
                id.toString(),
                "Story published and visible on app"
            )
            
            log.info("Story published: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.ok(
                StoryWorkflowResponse(
                    success = true,
                    message = "Story published successfully. Now visible to users on the app.",
                    status = "PUBLISHED",
                    storyId = id
                )
            )
        } else {
            log.warn("Failed to publish story: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.badRequest().body(
                StoryWorkflowResponse(
                    success = false,
                    message = "Cannot publish story. Story must be in AUDIO_REVIEW status and have audio available.",
                    storyId = id
                )
            )
        }
    }
    
    /**
     * Unpublish story.
     * 
     * Transitions story from PUBLISHED to DRAFT.
     * Removes story from user-facing app.
     * 
     * Valid only when story is in PUBLISHED status.
     * 
     * @param id Story ID to unpublish
     * @return Success response with new status
     */
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun unpublishStory(@PathVariable id: Long): ResponseEntity<StoryWorkflowResponse> {
        log.info("Unpublishing story: storyId={}", id)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        
        return if (storyLibraryService.unpublishStory(id)) {
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_unpublish_story",
                "story",
                id.toString(),
                "Story unpublished and removed from app"
            )
            
            log.info("Story unpublished: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.ok(
                StoryWorkflowResponse(
                    success = true,
                    message = "Story unpublished successfully. No longer visible to users.",
                    status = "DRAFT",
                    storyId = id
                )
            )
        } else {
            log.warn("Failed to unpublish story: storyId={}, admin={}", id, adminEmail)
            
            ResponseEntity.badRequest().body(
                StoryWorkflowResponse(
                    success = false,
                    message = "Cannot unpublish story. Story must be in PUBLISHED status.",
                    storyId = id
                )
            )
        }
    }
    
    /**
     * Approve audio for a specific language.
     * 
     * Marks audio as approved for the specified language.
     * When all required languages have approved audio, story transitions to AUDIO_REVIEW status.
     * 
     * Valid only when story is in APPROVED status and audio exists for the language.
     * 
     * @param id Story ID
     * @param language Language code (ta, en, hi, te, kn, ml)
     * @return Success response with approval status
     */
    @PostMapping("/{id}/audio/{language}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun approveAudioForLanguage(
        @PathVariable id: Long,
        @PathVariable language: String
    ): ResponseEntity<com.tamixa.api.library.dto.AudioReviewResponse> {
        log.info("Approving audio for language: storyId={}, language={}", id, language)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        
        return if (storyLibraryService.approveAudioForLanguage(id, language)) {
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_approve_audio_language",
                "story",
                id.toString(),
                "Audio approved for language: $language"
            )
            
            val approvalStatus = storyLibraryService.getAudioApprovalStatus(id)
            val approved = approvalStatus.filter { it.value != null }.keys.toList()
            val pending = approvalStatus.filter { it.value == null }.keys.toList()
            
            log.info("Audio approved: storyId={}, language={}, admin={}", id, language, adminEmail)
            
            ResponseEntity.ok(
                com.tamixa.api.library.dto.AudioReviewResponse(
                    storyId = id,
                    language = language,
                    success = true,
                    message = "Audio approved for $language. ${if (pending.isEmpty()) "All languages approved." else "${pending.size} language(s) pending."}",
                    status = if (pending.isEmpty()) "AUDIO_REVIEW" else "APPROVED",
                    approvedAt = java.time.Instant.now(),
                    languagesApproved = approved,
                    languagesPending = pending
                )
            )
        } else {
            log.warn("Failed to approve audio: storyId={}, language={}, admin={}", id, language, adminEmail)
            
            ResponseEntity.badRequest().body(
                com.tamixa.api.library.dto.AudioReviewResponse(
                    storyId = id,
                    language = language,
                    success = false,
                    message = "Cannot approve audio. Story must be in APPROVED status and audio must exist for this language."
                )
            )
        }
    }
    
    /**
     * Reject audio for a specific language.
     * 
     * Marks audio as rejected and optionally triggers regeneration.
     * Clears approval timestamp and optionally deletes audio to force regeneration.
     * 
     * Valid only when story is in APPROVED or AUDIO_REVIEW status.
     * 
     * @param id Story ID
     * @param language Language code (ta, en, hi, te, kn, ml)
     * @param request Optional request body with rejection notes and regeneration flag
     * @return Success response with rejection status
     */
    @PostMapping("/{id}/audio/{language}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun rejectAudioForLanguage(
        @PathVariable id: Long,
        @PathVariable language: String,
        @Valid @RequestBody(required = false) request: com.tamixa.api.library.dto.AudioReviewRequest?
    ): ResponseEntity<com.tamixa.api.library.dto.AudioReviewResponse> {
        log.info("Rejecting audio for language: storyId={}, language={}", id, language)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        val notes = request?.notes?.trim()?.takeIf { it.isNotBlank() }
        val forceRegenerate = request?.forceRegenerate ?: true
        
        return if (storyLibraryService.rejectAudioForLanguage(id, language, forceRegenerate)) {
            adminService.recordAdminAuditAction(
                adminEmail,
                "library_reject_audio_language",
                "story",
                id.toString(),
                "Audio rejected for language: $language. ${notes ?: "No notes provided."}"
            )
            
            log.info("Audio rejected: storyId={}, language={}, admin={}, regenerate={}", 
                id, language, adminEmail, forceRegenerate)
            
            ResponseEntity.ok(
                com.tamixa.api.library.dto.AudioReviewResponse(
                    storyId = id,
                    language = language,
                    success = true,
                    message = "Audio rejected for $language. ${if (forceRegenerate) "Audio will be regenerated." else ""}",
                    status = "APPROVED"
                )
            )
        } else {
            log.warn("Failed to reject audio: storyId={}, language={}, admin={}", id, language, adminEmail)
            
            ResponseEntity.badRequest().body(
                com.tamixa.api.library.dto.AudioReviewResponse(
                    storyId = id,
                    language = language,
                    success = false,
                    message = "Cannot reject audio. Story must be in APPROVED or AUDIO_REVIEW status."
                )
            )
        }
    }
    
    /**
     * Approve audio for all languages.
     * 
     * Bulk operation that approves audio for all languages that have READY audio.
     * Transitions story to AUDIO_REVIEW when all required languages are approved.
     * 
     * Valid only when story is in APPROVED status.
     * 
     * @param id Story ID
     * @return Success response with approval status for all languages
     */
    @PostMapping("/{id}/audio/approve-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_MANAGER', 'SUPER_ADMIN')")
    fun approveAudioForAllLanguages(@PathVariable id: Long): ResponseEntity<com.tamixa.api.library.dto.AudioReviewResponse> {
        log.info("Approving audio for all languages: storyId={}", id)
        
        val adminEmail = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        
        val results = storyLibraryService.approveAudioForAllLanguages(id)
        
        if (results.isEmpty()) {
            log.warn("Failed to approve audio for all languages: storyId={}, admin={}", id, adminEmail)
            
            return ResponseEntity.badRequest().body(
                com.tamixa.api.library.dto.AudioReviewResponse(
                    storyId = id,
                    success = false,
                    message = "Cannot approve audio. Story must be in APPROVED status."
                )
            )
        }
        
        val approved = results.filter { it.value }.keys.toList()
        val failed = results.filter { !it.value }.keys.toList()
        
        adminService.recordAdminAuditAction(
            adminEmail,
            "library_approve_audio_all",
            "story",
            id.toString(),
            "Audio approved for ${approved.size} language(s): ${approved.joinToString(", ")}"
        )
        
        log.info("Audio approved for all languages: storyId={}, approved={}, failed={}, admin={}", 
            id, approved.size, failed.size, adminEmail)
        
        return ResponseEntity.ok(
            com.tamixa.api.library.dto.AudioReviewResponse(
                storyId = id,
                success = true,
                message = "Audio approved for ${approved.size} language(s). ${if (failed.isNotEmpty()) "${failed.size} language(s) skipped (no audio)." else ""}",
                status = if (failed.isEmpty()) "AUDIO_REVIEW" else "APPROVED",
                languagesApproved = approved,
                languagesPending = failed
            )
        )
    }
}
