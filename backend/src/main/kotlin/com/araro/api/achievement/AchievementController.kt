package com.araro.api.achievement

import com.araro.api.ApiVersion
import com.araro.application.achievement.AchievementDefinitionDto
import com.araro.application.achievement.AchievementDto
import com.araro.application.achievement.AchievementService
import com.araro.application.child.ChildService
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
    private val childService: ChildService
) {

    @GetMapping("/definitions")
    fun getDefinitions(): ResponseEntity<List<AchievementDefinitionDto>> =
        ResponseEntity.ok(achievementService.getDefinitions())

    @GetMapping("/children/{childId}")
    fun getByChild(@PathVariable childId: Long): ResponseEntity<List<AchievementDto>> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return ResponseEntity.status(401).build()
        childService.findByIdForParent(childId, email)
        return ResponseEntity.ok(achievementService.getByChildId(childId))
    }
}
