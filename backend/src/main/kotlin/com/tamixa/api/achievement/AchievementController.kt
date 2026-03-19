package com.tamixa.api.achievement

import com.tamixa.api.ApiVersion
import com.tamixa.application.achievement.AchievementDefinitionDto
import com.tamixa.application.achievement.AchievementDto
import com.tamixa.application.achievement.AchievementService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/achievements")
@PreAuthorize("hasRole('PARENT')")
class AchievementController(
    private val achievementService: AchievementService,
    private val parentRepository: com.tamixa.application.port.ParentRepositoryPort
) {

    @GetMapping("/definitions")
    fun getDefinitions(): ResponseEntity<List<AchievementDefinitionDto>> =
        ResponseEntity.ok(achievementService.getDefinitions())

    /** Achievements for current parent (earned when story_completed count meets goal). */
    @GetMapping("/me")
    fun getMe(): ResponseEntity<List<AchievementDto>> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return ResponseEntity.notFound().build()
        val parent = parentRepository.findByEmail(email) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(achievementService.getByParentId(parent.id))
    }

    /** Child feature removed; returns empty list for backward compatibility. */
    @GetMapping("/children/{childId}")
    fun getByChild(@PathVariable childId: Long): ResponseEntity<List<AchievementDto>> =
        ResponseEntity.ok(emptyList())
}
