package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SoundscapeUsageJpaRepository : JpaRepository<SoundscapeUsageEntity, Long> {
    fun findByParentId(parentId: Long): List<SoundscapeUsageEntity>
    fun findByStoryId(storyId: Long): List<SoundscapeUsageEntity>
    fun findByParentIdAndSoundscapeId(parentId: Long, soundscapeId: Long): SoundscapeUsageEntity?

    @Modifying
    @Query(
        value = """
        INSERT INTO soundscapes_usage (parent_id, story_id, soundscape_id, usage_count, last_used_at, created_at, updated_at)
        VALUES (:parentId, :storyId, :soundscapeId, 1, :now, :now, :now)
        ON CONFLICT (parent_id, soundscape_id) DO UPDATE
        SET usage_count = soundscapes_usage.usage_count + 1,
            last_used_at = :now,
            updated_at = :now
        """,
        nativeQuery = true
    )
    fun incrementUsage(
        @Param("parentId") parentId: Long,
        @Param("storyId") storyId: Long?,
        @Param("soundscapeId") soundscapeId: Long,
        @Param("now") now: Instant
    ): Int
}
