package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReadingLevelJpaRepository : JpaRepository<ReadingLevelEntity, Long> {
    @Query("SELECT r FROM ReadingLevelEntity r WHERE r.child.id = :childId")
    fun findByChildId(@Param("childId") childId: Long): ReadingLevelEntity?
}

interface ReadingLevelAssessmentJpaRepository : JpaRepository<ReadingLevelAssessmentEntity, Long> {
    @Query("SELECT a FROM ReadingLevelAssessmentEntity a WHERE a.child.id = :childId ORDER BY a.assessedAt DESC")
    fun findByChildIdOrderByAssessedAtDesc(@Param("childId") childId: Long): List<ReadingLevelAssessmentEntity>
}
