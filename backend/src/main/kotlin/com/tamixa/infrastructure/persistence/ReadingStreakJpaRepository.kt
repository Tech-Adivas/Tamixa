package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReadingStreakJpaRepository : JpaRepository<ReadingStreakEntity, Long> {
    @Query("SELECT s FROM ReadingStreakEntity s WHERE s.child.id = :childId")
    fun findByChildId(@Param("childId") childId: Long): ReadingStreakEntity?
}
