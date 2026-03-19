package com.tamixa.api.narration

import com.tamixa.api.ApiVersion
import com.tamixa.api.narration.dto.NarrationRequestDto
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.StoryPipelineEventPublisherPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Narration API: request generation with tone mode and voice profiles.
 * Premium voices validated via subscription.voicePremium.
 * Triggers async Kafka pipeline; does not block until generation completes.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/narration")
@PreAuthorize("hasRole('PARENT')")
class NarrationController(
    private val translationRepository: StoryTranslationRepositoryPort,
    private val pipelinePublisher: StoryPipelineEventPublisherPort,
    private val parentRepository: ParentRepositoryPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/request")
    fun requestNarration(@Valid @RequestBody request: NarrationRequestDto): ResponseEntity<Unit> {
        log.info("Narration request translationId={} toneMode={} voiceProfiles={}", request.translationId, request.toneMode, request.voiceProfiles)
        if (translationRepository.findById(request.translationId) == null) {
            log.warn("Narration request rejected: translationId={} not found", request.translationId)
            return ResponseEntity.notFound().build()
        }
        val parentId = currentParentId() ?: run {
            log.warn("Narration request rejected: unauthorized")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        pipelinePublisher.publishNarrationRequest(
            translationId = request.translationId,
            toneMode = request.toneMode,
            voiceProfiles = request.voiceProfiles,
            parentId = parentId
        )
        log.info("Narration request accepted translationId={} parentId={}", request.translationId, parentId)
        return ResponseEntity.status(HttpStatus.ACCEPTED).build()
    }

    private fun currentParentId(): Long? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return parentRepository.findByEmail(email)?.id
    }
}
