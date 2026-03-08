package com.araro.infrastructure.persistence

import com.araro.domain.ProcessingStage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface StoryProcessingStatusJpaRepository : JpaRepository<StoryProcessingStatusEntity, Long> {

    fun findByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): StoryProcessingStatusEntity?

    fun findByMasterStoryId(masterStoryId: Long): List<StoryProcessingStatusEntity>

    fun findByStage(stage: ProcessingStage, pageable: Pageable): Page<StoryProcessingStatusEntity>
}
