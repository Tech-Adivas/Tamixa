package com.tamixa.api.education

import com.tamixa.api.ApiVersion
import com.tamixa.api.education.dto.ReadingStreakResponse
import com.tamixa.api.education.dto.StreakStatsResponse
import com.tamixa.application.service.ReadingStreakService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/reading-streaks")
@PreAuthorize("hasRole('PARENT')")
class ReadingStreakController(
    private val readingStreakService: ReadingStreakService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/{childId}")
    fun getStreak(@PathVariable childId: Long): ResponseEntity<ReadingStreakResponse> {
        log.debug("Get reading streak for child={}", childId)
        val streak = readingStreakService.getOrCreateStreak(childId)
        return ResponseEntity.ok(ReadingStreakResponse.from(streak))
    }

    @PostMapping("/{childId}/record-read")
    fun recordStoryRead(@PathVariable childId: Long): ResponseEntity<ReadingStreakResponse> {
        log.debug("Record story read for child={}", childId)
        val streak = readingStreakService.recordStoryRead(childId)
        return ResponseEntity.ok(ReadingStreakResponse.from(streak))
    }

    @GetMapping("/{childId}/stats")
    fun getStreakStats(@PathVariable childId: Long): ResponseEntity<StreakStatsResponse> {
        log.debug("Get streak stats for child={}", childId)
        val stats = readingStreakService.getStreakStats(childId)
        return ResponseEntity.ok(
            StreakStatsResponse(
                currentStreak = stats["currentStreak"] as Int,
                longestStreak = stats["longestStreak"] as Int,
                lastReadAt = stats["lastReadAt"] as String,
                isActive = stats["isActive"] as Boolean
            )
        )
    }
}
