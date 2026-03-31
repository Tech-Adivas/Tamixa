package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface StoryAnalyticsJpaRepository : JpaRepository<StoryAnalyticsEntity, Long> {

    /**
     * Distinct calendar days (UTC) when parent had at least one story_started event.
     * Used for listening streak computation.
     */
    @Query(
        value = """
        SELECT DISTINCT CAST((timestamp AT TIME ZONE 'UTC') AS date)
        FROM story_analytics
        WHERE parent_id = :parentId AND event_type = 'story_started' AND timestamp >= :since
        ORDER BY 1 DESC
        """,
        nativeQuery = true
    )
    fun findDistinctListeningDaysByParentIdSince(@Param("parentId") parentId: Long, @Param("since") since: Instant): List<java.sql.Date>

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
            SELECT s.theme, CAST(COUNT(*) AS bigint) as cnt FROM story_analytics a
            JOIN stories s ON s.id = a.story_id AND a.story_source = 'generated'
            WHERE a.event_type = 'story_started' AND a.timestamp >= :since
            GROUP BY s.theme
            UNION ALL
            SELECT c.theme, CAST(COUNT(*) AS bigint) as cnt FROM story_analytics a
            JOIN library_stories c ON c.id = a.story_id AND a.story_source = 'library' AND c.deleted_at IS NULL
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
    fun deleteByParent_Id(parentId: Long)

    /**
     * Top library story IDs by story_started count (play count).
     * Tamil: filter by c.language = 'ta'. Other languages: filter by translation existence.
     * Used for "Popular" home section.
     */
    @Query(
        value = """
        SELECT a.story_id FROM story_analytics a
        JOIN library_stories c ON c.id = a.story_id AND a.story_source = 'library' AND c.deleted_at IS NULL
        WHERE a.event_type = 'story_started' AND a.timestamp >= :since
          AND c.language = :language AND c.narration_approved_at IS NOT NULL
        GROUP BY a.story_id
        ORDER BY COUNT(*) DESC
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findPopularLibraryStoryIdsTamil(
        @Param("language") language: String,
        @Param("since") since: Instant,
        @Param("limit") limit: Int
    ): List<Long>

    /**
     * Top library story IDs (for non-Tamil) by story_started count.
     * Joins with story_translations to ensure translation exists in the requested language.
     */
    @Query(
        value = """
        SELECT a.story_id FROM story_analytics a
        JOIN library_stories c ON c.id = a.story_id AND a.story_source = 'library' AND c.deleted_at IS NULL
        JOIN story_translations t ON t.master_story_id = c.id AND t.language = :language
        WHERE a.event_type = 'story_started' AND a.timestamp >= :since
          AND c.narration_approved_at IS NOT NULL
        GROUP BY a.story_id
        ORDER BY COUNT(*) DESC
        LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findPopularLibraryStoryIdsByTranslationLanguage(
        @Param("language") language: String,
        @Param("since") since: Instant,
        @Param("limit") limit: Int
    ): List<Long>
}
