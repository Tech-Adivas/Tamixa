package com.araro.api.analytics

import com.araro.api.ApiVersion
import com.araro.application.analytics.StoryAnalyticsEventType
import com.araro.application.port.ParentRepositoryPort
import com.araro.infrastructure.persistence.StoryAnalyticsJpaRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/listening-progress")
@PreAuthorize("hasRole('PARENT')")
class ListeningProgressController(
    private val parentRepository: ParentRepositoryPort,
    private val storyAnalyticsJpaRepository: StoryAnalyticsJpaRepository
) {

    @GetMapping
    fun getProgress(@RequestParam(defaultValue = "30") days: Int): ResponseEntity<ListeningProgressResponse> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return ResponseEntity.notFound().build()
        val parent = parentRepository.findByEmail(email) ?: return ResponseEntity.notFound().build()
        val started = storyAnalyticsJpaRepository.countByParent_IdAndEventType(parent.id, StoryAnalyticsEventType.STORY_STARTED)
        val completed = storyAnalyticsJpaRepository.countByParent_IdAndEventType(parent.id, StoryAnalyticsEventType.STORY_COMPLETED)
        return ResponseEntity.ok(
            ListeningProgressResponse(
                periodDays = days,
                storiesStarted = started,
                storiesCompleted = completed,
                completionRate = if (started > 0) completed.toDouble() / started else 0.0
            )
        )
    }
}

data class ListeningProgressResponse(
    val periodDays: Int,
    val storiesStarted: Long,
    val storiesCompleted: Long,
    val completionRate: Double
)
