package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.domain.StoryTranslation
import com.tamixa.domain.TranslationPipelineStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class StoryTranslationRepositoryAdapter(
    private val jpaRepository: StoryTranslationJpaRepository
) : StoryTranslationRepositoryPort {

    override fun findById(id: Long): StoryTranslation? =
        jpaRepository.findActiveById(id)?.toDomain()

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
            lastError = translation.lastError,
            narrationApprovedAt = translation.narrationApprovedAt
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

    override fun findByLanguageAndMasterNarrationApproved(language: String, pageable: Pageable): Page<StoryTranslation> =
        jpaRepository.findByLanguageAndMasterNarrationApproved(language, pageable).map { it.toDomain() }

    override fun findByLanguageAndMasterNarrationApprovedWithMasterAudio(language: String, pageable: Pageable): Page<StoryTranslation> =
        jpaRepository.findByLanguageAndMasterNarrationApprovedWithMasterAudio(language, pageable).map { it.toDomain() }

    override fun findListingByLanguage(language: String, pageable: Pageable): Page<com.tamixa.domain.StoryTranslationListing> =
        jpaRepository.findListingByLanguage(language, pageable).map(::toListing)

    override fun findListingByLanguageAndMasterNarrationApproved(language: String, pageable: Pageable): Page<com.tamixa.domain.StoryTranslationListing> =
        jpaRepository.findListingByLanguageAndMasterNarrationApproved(language, pageable).map(::toListing)

    override fun findListingByLanguageAndMasterNarrationApprovedWithMasterAudio(language: String, pageable: Pageable): Page<com.tamixa.domain.StoryTranslationListing> =
        jpaRepository.findListingByLanguageAndMasterNarrationApprovedWithMasterAudio(language, pageable).map(::toListing)

    override fun findListingByLanguageAndMasterNarrationApprovedAndTheme(language: String, theme: String, pageable: Pageable): Page<com.tamixa.domain.StoryTranslationListing> =
        jpaRepository.findListingByLanguageAndMasterNarrationApprovedAndTheme(language, theme, pageable).map(::toListing)

    override fun findByLanguageAndMasterNarrationApprovedAndTheme(language: String, theme: String, pageable: Pageable): Page<StoryTranslation> =
        jpaRepository.findByLanguageAndMasterNarrationApprovedAndTheme(language, theme, pageable).map { it.toDomain() }

    override fun findByLanguageAndMasterNarrationApprovedWithMasterAudioAndTheme(
        language: String,
        theme: String,
        pageable: Pageable
    ): Page<StoryTranslation> =
        jpaRepository.findByLanguageAndMasterNarrationApprovedWithMasterAudioAndTheme(language, theme, pageable).map { it.toDomain() }

    override fun findByLanguageAndMasterNarrationApprovedAndLearnPrefix(
        language: String,
        prefix: String,
        pageable: Pageable
    ): Page<StoryTranslation> =
        jpaRepository.findByLanguageAndMasterNarrationApprovedAndLearnPrefix(language, prefix, pageable).map { it.toDomain() }

    override fun findByLanguageAndMasterNarrationApprovedWithMasterAudioAndLearnPrefix(
        language: String,
        prefix: String,
        pageable: Pageable
    ): Page<StoryTranslation> =
        jpaRepository.findByLanguageAndMasterNarrationApprovedWithMasterAudioAndLearnPrefix(language, prefix, pageable).map { it.toDomain() }

    override fun findByMasterStoryId(masterStoryId: Long): List<StoryTranslation> =
        jpaRepository.findByMasterStoryId(masterStoryId).map { it.toDomain() }

    override fun findByStatusIn(statuses: List<com.tamixa.domain.TranslationPipelineStatus>): List<StoryTranslation> =
        jpaRepository.findByStatusIn(statuses).map { it.toDomain() }

    override fun findRetryableByStatusIn(
        statuses: List<TranslationPipelineStatus>,
        maxRetriesExclusive: Int,
        pageable: Pageable
    ): Page<StoryTranslation> =
        jpaRepository.findRetryableByStatusIn(statuses, maxRetriesExclusive, pageable).map { it.toDomain() }

    override fun atomicStatusUpdate(id: Long, newStatus: com.tamixa.domain.TranslationPipelineStatus, lastError: String?): Boolean =
        jpaRepository.atomicStatusUpdate(id, newStatus, lastError) > 0

    override fun incrementRetryCount(id: Long, lastError: String): Boolean =
        jpaRepository.incrementRetryCount(id, lastError) > 0

    override fun resetRetryCountByMasterStoryId(masterStoryId: Long): Int =
        jpaRepository.resetRetryCountByMasterStoryId(masterStoryId)

    override fun resetRetryCountByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Int =
        jpaRepository.resetRetryCountByMasterStoryIdAndLanguage(masterStoryId, language)

    override fun resetPipelineStateForMasterStory(masterStoryId: Long): Int =
        jpaRepository.resetPipelineStateForMasterStory(masterStoryId, TranslationPipelineStatus.PENDING)

    override fun deleteByMasterStoryId(masterStoryId: Long) {
        jpaRepository.deleteByMasterStoryId(masterStoryId)
    }

    override fun updateNarrationApprovedAt(masterStoryId: Long, language: String, approvedAt: java.time.Instant?) {
        val entity = jpaRepository.findByMasterStoryIdAndLanguage(masterStoryId, language)
            ?: jpaRepository.findByMasterStoryIdAndLanguageIgnoreCase(masterStoryId, language)
            ?: return
        entity.narrationApprovedAt = approvedAt
        jpaRepository.save(entity)
    }

    override fun getNarrationApprovalByMasterStoryId(masterStoryId: Long): Map<String, Boolean> =
        jpaRepository.findByMasterStoryId(masterStoryId).associate { it.language to (it.narrationApprovedAt != null) }
}

private fun toListing(p: StoryTranslationListingProjection) = com.tamixa.domain.StoryTranslationListing(
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
    lastError = lastError,
    narrationApprovedAt = narrationApprovedAt
)
