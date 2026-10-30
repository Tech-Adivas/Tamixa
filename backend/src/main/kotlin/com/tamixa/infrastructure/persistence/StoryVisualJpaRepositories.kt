package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface StoryVisualSetJpaRepository : JpaRepository<StoryVisualSetEntity, Long> {

    fun findByStoryId(storyId: Long): StoryVisualSetEntity?

    @Modifying
    @Query("DELETE FROM StoryVisualSetEntity s WHERE s.storyId = :storyId")
    fun deleteByStoryIdQuery(@Param("storyId") storyId: Long): Int
}

interface StoryVisualImageJpaRepository : JpaRepository<StoryVisualImageEntity, Long> {

    fun findBySetIdOrderByImageKey(setId: Long): List<StoryVisualImageEntity>

    @Modifying
    @Query("DELETE FROM StoryVisualImageEntity i WHERE i.setId = :setId")
    fun deleteBySetIdQuery(@Param("setId") setId: Long): Int
}

interface StoryVisualShotJpaRepository : JpaRepository<StoryVisualShotEntity, Long> {

    fun findBySetIdOrderByShotIndex(setId: Long): List<StoryVisualShotEntity>

    @Modifying
    @Query("DELETE FROM StoryVisualShotEntity s WHERE s.setId = :setId")
    fun deleteBySetIdQuery(@Param("setId") setId: Long): Int
}
