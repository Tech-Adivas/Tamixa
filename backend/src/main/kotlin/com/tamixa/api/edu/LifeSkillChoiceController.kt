package com.tamixa.api.edu

import com.tamixa.api.ApiVersion
import com.tamixa.api.edu.dto.LifeSkillChoiceRequest
import com.tamixa.api.edu.dto.LifeSkillCountersResponse
import com.tamixa.application.edu.LifeSkillChoiceService
import com.tamixa.infrastructure.persistence.ParentJpaRepository
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Records tagged outcomes from interactive library episodes for future parent/educator tooling.
 * Does not return aggregate scores (product/legal review first).
 */
@RestController
@RequestMapping("${ApiVersion.V1}/edu/life-skill-choices")
@PreAuthorize("hasRole('PARENT')")
class LifeSkillChoiceController(
    private val lifeSkillChoiceService: LifeSkillChoiceService,
    private val parentJpaRepository: ParentJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Soft counters rolled up from choice [skillDeltas] (four pillars). Not a certificate or grade.
     */
    @GetMapping("/counters")
    fun getCounters(@RequestParam childId: Long): ResponseEntity<LifeSkillCountersResponse> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).build()
        if (parentJpaRepository.findByEmail(email) == null) {
            return ResponseEntity.status(401).build()
        }
        return try {
            ResponseEntity.ok(lifeSkillChoiceService.getSoftCounters(email, childId))
        } catch (e: IllegalArgumentException) {
            log.warn("Life skill counters rejected: {}", e.message)
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping
    fun record(@Valid @RequestBody request: LifeSkillChoiceRequest): ResponseEntity<Map<String, String>> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).build()
        if (parentJpaRepository.findByEmail(email) == null) {
            return ResponseEntity.status(401).build()
        }
        return try {
            lifeSkillChoiceService.recordChoice(
                parentEmail = email,
                libraryStoryId = request.libraryStoryId,
                childId = request.childId,
                segmentId = request.segmentId,
                choiceId = request.choiceId,
                skillDeltas = request.skillDeltas,
            )
            ResponseEntity.accepted().body(mapOf("status" to "recorded"))
        } catch (e: IllegalArgumentException) {
            log.warn("Life skill choice rejected: {}", e.message)
            ResponseEntity.badRequest().body(mapOf("message" to (e.message ?: "Invalid request")))
        }
    }
}
