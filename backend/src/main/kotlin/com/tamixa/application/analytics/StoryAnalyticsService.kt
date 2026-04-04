package com.tamixa.application.analytics

import com.tamixa.application.achievement.AchievementService
import com.tamixa.infrastructure.persistence.ChildJpaRepository
import com.tamixa.infrastructure.persistence.ParentJpaRepository
import com.tamixa.infrastructure.persistence.StoryAnalyticsEntity
import com.tamixa.infrastructure.persistence.StoryAnalyticsJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

object StoryAnalyticsEventType {
    const val STORY_STARTED = "story_started"
    const val STORY_25_PERCENT = "story_25_percent"
    const val STORY_50_PERCENT = "story_50_percent"
    const val STORY_75_PERCENT = "story_75_percent"
    const val STORY_COMPLETED = "story_completed"
    const val STORY_STOPPED_EARLY = "story_stopped_early"
    const val INTERACTIVE_BRANCH = "interactive_branch"
    const val SHARE_CLIP_REQUESTED = "share_clip_requested"
    const val SHARE_CLIP_DOWNLOADED = "share_clip_downloaded"

    val ALL = setOf(
        STORY_STARTED, STORY_25_PERCENT, STORY_50_PERCENT, STORY_75_PERCENT,
        STORY_COMPLETED, STORY_STOPPED_EARLY, INTERACTIVE_BRANCH,
        SHARE_CLIP_REQUESTED, SHARE_CLIP_DOWNLOADED
    )
}

@Service
class StoryAnalyticsService(
    private val storyAnalyticsJpaRepository: StoryAnalyticsJpaRepository,
    private val parentJpaRepository: ParentJpaRepository,
    private val childJpaRepository: ChildJpaRepository,
    private val achievementService: AchievementService
) {

    @Transactional
    fun trackEvent(
        parentId: Long,
        storyId: Long,
        storySource: String,
        language: String,
        eventType: String,
        playbackPositionSeconds: Int,
        childId: Long? = null
    ) {
        if (eventType !in StoryAnalyticsEventType.ALL) return
        val parent = parentJpaRepository.findById(parentId).orElse(null) ?: return
        val source = if (storySource.lowercase() == "library") "library" else "generated"
        val child = childId?.let { childJpaRepository.findById(it).orElse(null) }
            ?.takeIf { it.parent.id == parentId }
        storyAnalyticsJpaRepository.save(
            StoryAnalyticsEntity(
                parent = parent,
                storyId = storyId,
                storySource = source,
                language = language,
                eventType = eventType,
                timestamp = Instant.now(),
                playbackPositionSeconds = playbackPositionSeconds.coerceAtLeast(0),
                child = child
            )
        )
        if (eventType == StoryAnalyticsEventType.STORY_COMPLETED && child != null) {
            achievementService.checkAndAwardOnCompletion(child.id)
        }
    }

    @Transactional(readOnly = true)
    fun getRetentionMetrics(days: Int): RetentionMetricsDto {
        val since = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)
        val avgListen = storyAnalyticsJpaRepository.averageListenTimeSeconds(since) ?: 0.0
        val byLanguage = storyAnalyticsJpaRepository.completionRateByLanguage(since)
            .associate { row -> (row[0] as String) to ((row[1] as? Number)?.toDouble() ?: 0.0) }
        val themeRows = storyAnalyticsJpaRepository.storyStartsByTheme(since)
        val mostPopularCategory = themeRows.maxByOrNull { (it[1] as? Number)?.toLong() ?: 0L }
            ?.let { it[0] as? String } ?: ""

        return RetentionMetricsDto(
            averageListenTimeSeconds = avgListen,
            mostPopularCategory = mostPopularCategory,
            retentionByLanguage = byLanguage,
            periodDays = days
        )
    }

    /**
     * Consecutive calendar days (UTC) with at least one story_started event.
     * Today counts if there was already an event today.
     */
    @Transactional(readOnly = true)
    fun getListeningStreakDays(parentId: Long): Int {
        val now = Instant.now()
        val today = now.atOffset(ZoneOffset.UTC).toLocalDate()
        val since = now.minus(365, ChronoUnit.DAYS)
        val dates = storyAnalyticsJpaRepository.findDistinctListeningDaysByParentIdSince(parentId, since)
            .map { it.toLocalDate() }
            .toSet()
        var streak = 0
        var current = today
        while (current in dates) {
            streak++
            current = current.minusDays(1)
        }
        return streak
    }

    @Transactional(readOnly = true)
    fun getCompletionMetrics(days: Int): CompletionMetricsDto {
        val since = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)
        val byStory = storyAnalyticsJpaRepository.completionRateByStory(since)
            .map { row ->
                CompletionRatePerStory(
                    storyId = (row[0] as Number).toLong(),
                    storySource = row[1] as? String ?: "generated",
                    completionRate = (row[2] as? Number)?.toDouble() ?: 0.0
                )
            }
        val byLanguage = storyAnalyticsJpaRepository.completionRateByLanguage(since)
            .associate { row -> (row[0] as String) to ((row[1] as? Number)?.toDouble() ?: 0.0) }
        val overall = byStory.let { list ->
            if (list.isEmpty()) 0.0
            else list.map { it.completionRate }.average()
        }

        return CompletionMetricsDto(
            completionRatePerStory = byStory,
            overallCompletionRate = overall,
            retentionByLanguage = byLanguage,
            periodDays = days
        )
    }
}

data class RetentionMetricsDto(
    val averageListenTimeSeconds: Double,
    val mostPopularCategory: String,
    val retentionByLanguage: Map<String, Double>,
    val periodDays: Int
)

data class CompletionMetricsDto(
    val completionRatePerStory: List<CompletionRatePerStory>,
    val overallCompletionRate: Double,
    val retentionByLanguage: Map<String, Double>,
    val periodDays: Int
)

data class CompletionRatePerStory(
    val storyId: Long,
    val storySource: String,
    val completionRate: Double
)
