package com.araro.infrastructure.persistence

import com.araro.domain.StoryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface StoryJpaRepository : JpaRepository<StoryEntity, Long> {

    fun findByStatus(status: StoryStatus, pageable: Pageable): Page<StoryEntity>

    fun findByStatusAndThemeContainingIgnoreCase(status: StoryStatus, theme: String, pageable: Pageable): Page<StoryEntity>

    fun findByThemeContainingIgnoreCase(theme: String, pageable: Pageable): Page<StoryEntity>

    fun findByParent_Id(parentId: Long, pageable: Pageable): Page<StoryEntity>

    fun countByParent_IdAndCreatedAtBetween(parentId: Long, start: Instant, end: Instant): Long

    @Query(
        value = """
            SELECT CAST((created_at AT TIME ZONE 'UTC') AS date) as day, COUNT(*) as cnt
            FROM stories
            WHERE created_at >= :since
            GROUP BY CAST((created_at AT TIME ZONE 'UTC') AS date)
            ORDER BY day
        """,
        nativeQuery = true
    )
    fun countByDaySince(@Param("since") since: Instant): List<Array<Any>>

    @Query(
        "SELECT s FROM StoryEntity s WHERE s.parent.id = :parentId " +
            "AND (LOWER(s.theme) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR (s.title IS NOT NULL AND LOWER(s.title) LIKE LOWER(CONCAT('%', :q, '%'))))"
    )
    fun searchByThemeOrTitle(@Param("parentId") parentId: Long, @Param("q") query: String, pageable: Pageable): Page<StoryEntity>
}
