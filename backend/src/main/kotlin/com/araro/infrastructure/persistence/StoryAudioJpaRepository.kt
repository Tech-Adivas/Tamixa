package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface StoryAudioJpaRepository : JpaRepository<StoryAudioEntity, Long> {

    fun findByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): StoryAudioEntity?

    fun existsByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Boolean

    fun deleteByMasterStoryIdAndLanguage(masterStoryId: Long, language: String)

    fun deleteByMasterStoryId(masterStoryId: Long)

    @Modifying
    @Query("DELETE FROM StoryAudioEntity")
    fun deleteAllLegacy(): Int
}
