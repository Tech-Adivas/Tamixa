package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface LifeSkillChoiceEventJpaRepository : JpaRepository<LifeSkillChoiceEventEntity, Long> {

    fun countByCreatedAtAfter(since: Instant): Long

    @Query(
        """
        SELECT e.libraryStoryId AS libraryStoryId, e.segmentId AS segmentId, e.choiceId AS choiceId, COUNT(e) AS eventCount
        FROM LifeSkillChoiceEventEntity e
        WHERE e.createdAt >= :since
        GROUP BY e.libraryStoryId, e.segmentId, e.choiceId
        ORDER BY COUNT(e) DESC
        """
    )
    fun aggregateSince(@Param("since") since: Instant): List<LifeSkillChoiceAggregateRow>
}
