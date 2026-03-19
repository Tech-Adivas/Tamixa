package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ChildAchievementJpaRepository : JpaRepository<ChildAchievementEntity, Long> {

    fun findByChild_Id(childId: Long): List<ChildAchievementEntity>

    fun existsByChild_IdAndAchievementType(childId: Long, achievementType: String): Boolean
    fun deleteByChild_Parent_Id(parentId: Long)
}
