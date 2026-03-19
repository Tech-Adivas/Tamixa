package com.tamixa.api.shareclip

import com.tamixa.api.ApiVersion
import com.tamixa.api.shareclip.dto.ShareClipQuotaResponse
import com.tamixa.api.shareclip.dto.ShareClipRequest
import com.tamixa.api.shareclip.dto.ShareClipResponse
import com.tamixa.application.shareclip.ShareClipQuotaResult
import com.tamixa.application.shareclip.ShareClipService
import com.tamixa.application.shareclip.ShareClipStatusResult
import com.tamixa.infrastructure.persistence.ParentJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid

@RestController
@RequestMapping("${ApiVersion.V1}")
@PreAuthorize("hasRole('PARENT')")
class ShareClipController(
    private val shareClipService: ShareClipService,
    private val parentJpaRepository: ParentJpaRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/stories/library/{id}/share-clip")
    fun requestLibraryShareClip(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(defaultValue = "default") voiceProfile: String,
        @Valid @RequestBody request: ShareClipRequest
    ): ResponseEntity<ShareClipResponse> {
        val parentId = currentParentId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return requestShareClip(parentId, id, "library", language, voiceProfile, request)
    }

    @PostMapping("/stories/generated/{id}/share-clip")
    fun requestGeneratedShareClip(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(defaultValue = "default") voiceProfile: String,
        @Valid @RequestBody request: ShareClipRequest
    ): ResponseEntity<ShareClipResponse> {
        val parentId = currentParentId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        return requestShareClip(parentId, id, "generated", language, voiceProfile, request)
    }

    private fun requestShareClip(
        parentId: Long,
        storyId: Long,
        storySource: String,
        language: String,
        voiceProfile: String,
        request: ShareClipRequest
    ): ResponseEntity<ShareClipResponse> {
        return try {
            val clip = shareClipService.requestClip(
                parentId = parentId,
                storyId = storyId,
                storySource = storySource,
                language = language,
                voiceProfile = voiceProfile,
                startSeconds = request.startSeconds,
                durationSeconds = request.durationSeconds,
                format = request.format ?: "9:16"
            )
            ResponseEntity.status(HttpStatus.ACCEPTED).body(
                ShareClipResponse(clipId = clip.id, status = clip.status)
            )
        } catch (e: com.tamixa.domain.subscription.UpgradeRequiredException) {
            log.debug("Share clip rejected: {}", e.message)
            ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(
                ShareClipResponse(clipId = 0, status = "UPGRADE_REQUIRED", errorMessage = e.message)
            )
        }
    }

    @GetMapping("/share-clips/{clipId}")
    fun getClipStatus(@PathVariable clipId: Long): ResponseEntity<ShareClipResponse> {
        val parentId = currentParentId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val result = shareClipService.getClipStatus(clipId, parentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(
            ShareClipResponse(
                clipId = result.clipId,
                status = result.status,
                downloadUrl = result.downloadUrl,
                errorMessage = result.errorMessage
            )
        )
    }

    @GetMapping("/share-clips/quota")
    fun getQuota(): ResponseEntity<ShareClipQuotaResponse> {
        val parentId = currentParentId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val result = shareClipService.getQuotaRemaining(parentId)
        return ResponseEntity.ok(
            ShareClipQuotaResponse(
                remaining = result.remaining,
                limit = result.limit,
                entitled = result.entitled
            )
        )
    }

    private fun currentParentId(): Long? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return parentJpaRepository.findByEmail(email)?.id
    }
}
