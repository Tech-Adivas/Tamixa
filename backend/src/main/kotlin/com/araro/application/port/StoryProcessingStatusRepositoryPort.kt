package com.araro.application.port

import com.araro.domain.ProcessingStage
import com.araro.domain.StoryProcessingStatus

interface StoryProcessingStatusRepositoryPort {

    fun save(status: StoryProcessingStatus): StoryProcessingStatus

    fun findByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): StoryProcessingStatus?

    fun findOrCreate(masterStoryId: Long, language: String, initialStage: ProcessingStage): StoryProcessingStatus

    fun updateStage(masterStoryId: Long, language: String, stage: ProcessingStage, lastError: String? = null): StoryProcessingStatus?

    fun incrementRetryCount(masterStoryId: Long, language: String, lastError: String): StoryProcessingStatus?
}
