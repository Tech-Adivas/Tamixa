package com.tamixa.api.recommendation

import com.tamixa.api.ApiVersion
import com.tamixa.application.recommendation.RecommendedStoryDto
import com.tamixa.application.recommendation.StoryRecommendationService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/stories")
@PreAuthorize("hasRole('PARENT')")
class StoryRecommendationController(
    private val recommendationService: StoryRecommendationService
) {

    @GetMapping("/recommended")
    fun getRecommended(
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) childId: Long?,
        @RequestParam(defaultValue = "15") limit: Int
    ): ResponseEntity<List<RecommendedStoryDto>> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(recommendationService.getRecommended(email, language, childId, limit))
    }
}
