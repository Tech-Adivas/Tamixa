package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface StoryVersionJpaRepository : JpaRepository<StoryVersionEntity, Long> {
    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) + 1 FROM StoryVersionEntity v WHERE v.libraryStoryId = :libraryStoryId")
    fun nextVersionNumber(libraryStoryId: Long): Int
}
