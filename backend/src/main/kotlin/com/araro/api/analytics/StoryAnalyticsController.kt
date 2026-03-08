package com.araro.api.analytics

import com.araro.api.ApiVersion
import com.araro.api.analytics.dto.TrackAppEventRequest
import com.araro.api.analytics.dto.TrackStoryEventRequest
import com.araro.application.analytics.StoryAnalyticsService
import com.araro.infrastructure.persistence.ParentJpaRepository
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/analytics")
@PreAuthorize("hasRole('PARENT')")
class StoryAnalyticsController(
    private val storyAnalyticsService: StoryAnalyticsService,
    private val parentJpaRepository: ParentJpaRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/app-events")
    fun trackAppEvent(@Valid @RequestBody request: TrackAppEventRequest): ResponseEntity<Unit> {
        val parentId = currentParentId()
        if (parentId == null) return ResponseEntity.status(401).build()
        log.debug(
            "App event parentId={} type={} screen={} searchLen={} storyId={}",
            parentId,
            request.eventType,
            request.screenName,
            request.searchQueryLength,
            request.storyId
        )
        return ResponseEntity.ok().build()
    }

    @PostMapping("/story-events")
    fun trackStoryEvent(@Valid @RequestBody request: TrackStoryEventRequest): ResponseEntity<Unit> {
        val parentId = currentParentId() ?: return ResponseEntity.status(401).build()
        storyAnalyticsService.trackEvent(
            parentId = parentId,
            storyId = request.storyId,
            storySource = request.storySource,
            language = request.language,
            eventType = request.eventType,
            playbackPositionSeconds = request.playbackPositionSeconds,
            childId = request.childId
        )
        return ResponseEntity.ok().build()
    }

    private fun currentParentId(): Long? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return parentJpaRepository.findByEmail(email)?.id
    }
}
