package com.araro.infrastructure.persistence

import com.araro.domain.TranslationPipelineStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface StoryTranslationJpaRepository : JpaRepository<StoryTranslationEntity, Long> {

    fun findByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): StoryTranslationEntity?

    fun findByMasterStoryIdAndLanguageIgnoreCase(masterStoryId: Long, language: String): StoryTranslationEntity?

    fun findByMasterStoryId(masterStoryId: Long, pageable: Pageable): Page<StoryTranslationEntity>

    fun findByLanguage(language: String, pageable: Pageable): Page<StoryTranslationEntity>

    fun findListingByLanguage(language: String, pageable: Pageable): Page<StoryTranslationListingProjection>

    fun existsByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Boolean

    fun findByMasterStoryId(masterStoryId: Long): List<StoryTranslationEntity>

    fun findByStatusIn(statuses: List<TranslationPipelineStatus>): List<StoryTranslationEntity>

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        "UPDATE StoryTranslationEntity e SET e.status = :newStatus, e.lastError = :lastError " +
            "WHERE e.id = :id"
    )
    fun atomicStatusUpdate(
        id: Long,
        newStatus: TranslationPipelineStatus,
        lastError: String?
    ): Int

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        "UPDATE StoryTranslationEntity e SET e.retryCount = e.retryCount + 1, e.lastError = :lastError " +
            "WHERE e.id = :id"
    )
    fun incrementRetryCount(id: Long, lastError: String): Int

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        "UPDATE StoryTranslationEntity e SET e.retryCount = 0, e.lastError = null WHERE e.masterStoryId = :masterStoryId"
    )
    fun resetRetryCountByMasterStoryId(masterStoryId: Long): Int

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        "UPDATE StoryTranslationEntity e SET e.retryCount = 0, e.lastError = null " +
            "WHERE e.masterStoryId = :masterStoryId AND e.language = :language"
    )
    fun resetRetryCountByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Int

    fun deleteByMasterStoryId(masterStoryId: Long)
}
