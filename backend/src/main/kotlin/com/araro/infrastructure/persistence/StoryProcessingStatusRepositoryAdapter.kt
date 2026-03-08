package com.araro.infrastructure.persistence

import com.araro.application.port.StoryProcessingStatusRepositoryPort
import com.araro.domain.ProcessingStage
import com.araro.domain.StoryProcessingStatus
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StoryProcessingStatusRepositoryAdapter(
    private val jpaRepository: StoryProcessingStatusJpaRepository
) : StoryProcessingStatusRepositoryPort {

    override fun save(status: StoryProcessingStatus): StoryProcessingStatus {
        val entity = StoryProcessingStatusEntity(
            id = status.id,
            masterStoryId = status.masterStoryId,
            language = status.language,
            stage = status.stage,
            retryCount = status.retryCount,
            lastError = status.lastError,
            updatedAt = status.updatedAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): StoryProcessingStatus? =
        jpaRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)?.toDomain()

    override fun findOrCreate(masterStoryId: Long, language: String, initialStage: ProcessingStage): StoryProcessingStatus {
        val existing = jpaRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)
        if (existing != null) return existing.toDomain()
        val now = Instant.now()
        val newStatus = StoryProcessingStatus(
            id = 0,
            masterStoryId = masterStoryId,
            language = language,
            stage = initialStage,
            retryCount = 0,
            lastError = null,
            updatedAt = now
        )
        return save(newStatus)
    }

    override fun updateStage(masterStoryId: Long, language: String, stage: ProcessingStage, lastError: String?): StoryProcessingStatus? {
        val entity = jpaRepository.findByMasterStoryIdAndLanguage(masterStoryId, language) ?: return null
        entity.stage = stage
        entity.lastError = lastError
        entity.updatedAt = Instant.now()
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun incrementRetryCount(masterStoryId: Long, language: String, lastError: String): StoryProcessingStatus? {
        val entity = jpaRepository.findByMasterStoryIdAndLanguage(masterStoryId, language) ?: return null
        entity.retryCount += 1
        entity.lastError = lastError
        entity.updatedAt = Instant.now()
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }
}

private fun StoryProcessingStatusEntity.toDomain() = StoryProcessingStatus(
    id = id,
    masterStoryId = masterStoryId,
    language = language,
    stage = stage,
    retryCount = retryCount,
    lastError = lastError,
    updatedAt = updatedAt
)
