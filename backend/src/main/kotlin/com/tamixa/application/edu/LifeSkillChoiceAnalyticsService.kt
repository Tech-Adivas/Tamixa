package com.tamixa.application.edu

import com.tamixa.api.admin.dto.LifeSkillChoiceAnalyticsResponse
import com.tamixa.api.admin.dto.LifeSkillChoiceAnalyticsRowResponse
import com.tamixa.infrastructure.persistence.LibraryStoryJpaRepository
import com.tamixa.infrastructure.persistence.LifeSkillChoiceEventJpaRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val MAX_ROWS = 500

@Service
class LifeSkillChoiceAnalyticsService(
    private val lifeSkillChoiceEventJpaRepository: LifeSkillChoiceEventJpaRepository,
    private val libraryStoryJpaRepository: LibraryStoryJpaRepository,
) {

    fun getAggregates(days: Int): LifeSkillChoiceAnalyticsResponse {
        val periodDays = days.coerceIn(1, 365)
        val since = Instant.now().minus(periodDays.toLong(), ChronoUnit.DAYS)
        val total = lifeSkillChoiceEventJpaRepository.countByCreatedAtAfter(since)
        val raw = lifeSkillChoiceEventJpaRepository.aggregateSince(since).take(MAX_ROWS)
        val storyIds = raw.map { it.getLibraryStoryId() }.distinct()
        val titlesById =
            if (storyIds.isEmpty()) {
                emptyMap()
            } else {
                libraryStoryJpaRepository.findAllById(storyIds).associate { it.id to (it.title ?: "") }
            }
        val rows =
            raw.map { row ->
                val id = row.getLibraryStoryId()
                val title = titlesById[id]?.takeIf { it.isNotBlank() }
                LifeSkillChoiceAnalyticsRowResponse(
                    libraryStoryId = id,
                    storyTitle = title,
                    segmentId = row.getSegmentId(),
                    choiceId = row.getChoiceId(),
                    eventCount = row.getEventCount(),
                )
            }
        return LifeSkillChoiceAnalyticsResponse(
            periodDays = periodDays,
            totalEvents = total,
            rows = rows,
        )
    }
}
