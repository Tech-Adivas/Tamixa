package com.araro.infrastructure.persistence

import com.araro.application.port.StoryTranslationRepositoryPort
import com.araro.domain.StoryTranslation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class StoryTranslationRepositoryAdapter(
    private val jpaRepository: StoryTranslationJpaRepository
) : StoryTranslationRepositoryPort {

    override fun findById(id: Long): StoryTranslation? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun save(translation: StoryTranslation): StoryTranslation {
        val entity = StoryTranslationEntity(
            id = translation.id,
            masterStoryId = translation.masterStoryId,
            language = translation.language,
            title = translation.title,
            content = translation.content,
            moral = translation.moral,
            wordCount = translation.wordCount,
            readingTimeMinutes = translation.readingTimeMinutes,
            createdAt = translation.createdAt,
            status = translation.status,
            retryCount = translation.retryCount,
            lastError = translation.lastError
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): StoryTranslation? =
        (jpaRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)
            ?: jpaRepository.findByMasterStoryIdAndLanguageIgnoreCase(masterStoryId, language))?.toDomain()

    override fun existsByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Boolean =
        jpaRepository.existsByMasterStoryIdAndLanguage(masterStoryId, language)

    override fun findByLanguage(language: String, pageable: Pageable): Page<StoryTranslation> =
        jpaRepository.findByLanguage(language, pageable).map { it.toDomain() }

    override fun findListingByLanguage(language: String, pageable: Pageable): Page<com.araro.domain.StoryTranslationListing> =
        jpaRepository.findListingByLanguage(language, pageable).map(::toListing)

    override fun findByMasterStoryId(masterStoryId: Long): List<StoryTranslation> =
        jpaRepository.findByMasterStoryId(masterStoryId).map { it.toDomain() }

    override fun findByStatusIn(statuses: List<com.araro.domain.TranslationPipelineStatus>): List<StoryTranslation> =
        jpaRepository.findByStatusIn(statuses).map { it.toDomain() }

    override fun atomicStatusUpdate(id: Long, newStatus: com.araro.domain.TranslationPipelineStatus, lastError: String?): Boolean =
        jpaRepository.atomicStatusUpdate(id, newStatus, lastError) > 0

    override fun incrementRetryCount(id: Long, lastError: String): Boolean =
        jpaRepository.incrementRetryCount(id, lastError) > 0

    override fun resetRetryCountByMasterStoryId(masterStoryId: Long): Int =
        jpaRepository.resetRetryCountByMasterStoryId(masterStoryId)

    override fun resetRetryCountByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Int =
        jpaRepository.resetRetryCountByMasterStoryIdAndLanguage(masterStoryId, language)

    override fun deleteByMasterStoryId(masterStoryId: Long) {
        jpaRepository.deleteByMasterStoryId(masterStoryId)
    }
}

private fun toListing(p: StoryTranslationListingProjection) = com.araro.domain.StoryTranslationListing(
    id = p.getId(),
    masterStoryId = p.getMasterStoryId(),
    title = p.getTitle(),
    language = p.getLanguage(),
    wordCount = p.getWordCount(),
    readingTimeMinutes = p.getReadingTimeMinutes(),
    createdAt = p.getCreatedAt()
)

private fun StoryTranslationEntity.toDomain() = StoryTranslation(
    id = id,
    masterStoryId = masterStoryId,
    language = language,
    title = title,
    content = content,
    moral = moral,
    wordCount = wordCount,
    readingTimeMinutes = readingTimeMinutes,
    createdAt = createdAt,
    status = status,
    retryCount = retryCount,
    lastError = lastError
)
