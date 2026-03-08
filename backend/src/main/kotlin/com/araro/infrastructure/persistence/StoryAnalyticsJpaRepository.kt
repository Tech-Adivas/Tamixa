package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface StoryAnalyticsJpaRepository : JpaRepository<StoryAnalyticsEntity, Long> {

    @Query(
        value = """
        SELECT AVG(max_pos) FROM (
            SELECT parent_id, story_id, DATE(timestamp at time zone 'UTC') as day,
                   MAX(playback_position_seconds) as max_pos
            FROM story_analytics
            WHERE timestamp >= :since
            GROUP BY parent_id, story_id, DATE(timestamp at time zone 'UTC')
        ) s WHERE max_pos > 0
        """,
        nativeQuery = true
    )
    fun averageListenTimeSeconds(since: Instant): Double?

    @Query(
        "SELECT a.language, " +
        "COUNT(CASE WHEN a.eventType = 'story_completed' THEN 1 END) * 1.0 / " +
        "NULLIF(COUNT(CASE WHEN a.eventType = 'story_started' THEN 1 END), 0) " +
        "FROM StoryAnalyticsEntity a WHERE a.timestamp >= :since GROUP BY a.language"
    )
    fun completionRateByLanguage(since: Instant): List<Array<Any>>

    @Query(
        "SELECT a.storyId, a.storySource, " +
        "COUNT(CASE WHEN a.eventType = 'story_completed' THEN 1 END) * 1.0 / " +
        "NULLIF(COUNT(CASE WHEN a.eventType = 'story_started' THEN 1 END), 0) " +
        "FROM StoryAnalyticsEntity a WHERE a.timestamp >= :since GROUP BY a.storyId, a.storySource"
    )
    fun completionRateByStory(since: Instant): List<Array<Any>>

    @Query(
        value = """
        SELECT theme, SUM(cnt) as total FROM (
            SELECT s.theme, COUNT(*)::bigint as cnt FROM story_analytics a
            JOIN stories s ON s.id = a.story_id AND a.story_source = 'generated'
            WHERE a.event_type = 'story_started' AND a.timestamp >= :since
            GROUP BY s.theme
            UNION ALL
            SELECT c.theme, COUNT(*)::bigint as cnt FROM story_analytics a
            JOIN curated_stories c ON c.id = a.story_id AND a.story_source = 'curated'
            WHERE a.event_type = 'story_started' AND a.timestamp >= :since
            GROUP BY c.theme
        ) u GROUP BY theme ORDER BY total DESC
        """,
        nativeQuery = true
    )
    fun storyStartsByTheme(since: Instant): List<Array<Any>>

    fun countByParent_IdAndEventType(parentId: Long, eventType: String): Long

    fun countByChild_IdAndEventType(childId: Long, eventType: String): Long

    fun deleteByStoryIdAndStorySource(storyId: Long, storySource: String)
}
