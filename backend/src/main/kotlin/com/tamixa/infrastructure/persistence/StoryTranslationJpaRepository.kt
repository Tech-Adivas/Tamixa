package com.tamixa.infrastructure.persistence

import com.tamixa.domain.TranslationPipelineStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface StoryTranslationJpaRepository : JpaRepository<StoryTranslationEntity, Long> {

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE t.id = :id AND c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL"
    )
    fun findActiveById(@Param("id") id: Long): StoryTranslationEntity?

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.masterStoryId = :masterStoryId AND t.language = :language"
    )
    fun findByMasterStoryIdAndLanguage(
        @Param("masterStoryId") masterStoryId: Long,
        @Param("language") language: String
    ): StoryTranslationEntity?

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.masterStoryId = :masterStoryId AND LOWER(t.language) = LOWER(:language)"
    )
    fun findByMasterStoryIdAndLanguageIgnoreCase(
        @Param("masterStoryId") masterStoryId: Long,
        @Param("language") language: String
    ): StoryTranslationEntity?

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.masterStoryId = :masterStoryId"
    )
    fun findByMasterStoryId(masterStoryId: Long, pageable: Pageable): Page<StoryTranslationEntity>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language"
    )
    fun findByLanguage(@Param("language") language: String, pageable: Pageable): Page<StoryTranslationEntity>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language AND c.narrationApprovedAt IS NOT NULL AND EXISTS (" +
            "SELECT 1 FROM StoryNarrationAudioEntity a WHERE a.translationId = t.id " +
            "AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0)"
    )
    fun findByLanguageAndMasterNarrationApproved(
        @Param("language") language: String,
        pageable: Pageable
    ): Page<StoryTranslationEntity>

    /**
     * Same as [findByLanguageAndMasterNarrationApproved] but narration readiness is tied to the **master** language row
     * (library_stories.language) or legacy master audio_file_url — not the requested translation's audio.
     */
    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language AND c.narrationApprovedAt IS NOT NULL AND (" +
            "EXISTS (" +
            "SELECT 1 FROM StoryTranslationEntity tm, StoryNarrationAudioEntity a " +
            "WHERE tm.masterStoryId = c.id AND LOWER(tm.language) = LOWER(c.language) " +
            "AND a.translationId = tm.id AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0" +
            ") OR (c.audioFileUrl IS NOT NULL AND LENGTH(c.audioFileUrl) > 0))"
    )
    fun findByLanguageAndMasterNarrationApprovedWithMasterAudio(
        @Param("language") language: String,
        pageable: Pageable
    ): Page<StoryTranslationEntity>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language AND c.narrationApprovedAt IS NOT NULL"
    )
    fun findListingByLanguageAndMasterNarrationApproved(
        @Param("language") language: String,
        pageable: Pageable
    ): Page<StoryTranslationListingProjection>

    /** Same as [findListingByLanguageAndMasterNarrationApproved] with master-language narration readiness (master-only mode). */
    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language AND c.narrationApprovedAt IS NOT NULL AND (" +
            "EXISTS (" +
            "SELECT 1 FROM StoryTranslationEntity tm, StoryNarrationAudioEntity a " +
            "WHERE tm.masterStoryId = c.id AND LOWER(tm.language) = LOWER(c.language) " +
            "AND a.translationId = tm.id AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0" +
            ") OR (c.audioFileUrl IS NOT NULL AND LENGTH(c.audioFileUrl) > 0))"
    )
    fun findListingByLanguageAndMasterNarrationApprovedWithMasterAudio(
        @Param("language") language: String,
        pageable: Pageable
    ): Page<StoryTranslationListingProjection>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language AND c.narrationApprovedAt IS NOT NULL " +
            "AND LOWER(c.theme) = LOWER(:theme)"
    )
    fun findListingByLanguageAndMasterNarrationApprovedAndTheme(
        @Param("language") language: String,
        @Param("theme") theme: String,
        pageable: Pageable
    ): Page<StoryTranslationListingProjection>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language AND c.narrationApprovedAt IS NOT NULL " +
            "AND LOWER(c.theme) = LOWER(:theme) AND EXISTS (" +
            "SELECT 1 FROM StoryNarrationAudioEntity a WHERE a.translationId = t.id " +
            "AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0)"
    )
    fun findByLanguageAndMasterNarrationApprovedAndTheme(
        @Param("language") language: String,
        @Param("theme") theme: String,
        pageable: Pageable
    ): Page<StoryTranslationEntity>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language AND c.narrationApprovedAt IS NOT NULL " +
            "AND LOWER(c.theme) = LOWER(:theme) AND (" +
            "EXISTS (" +
            "SELECT 1 FROM StoryTranslationEntity tm, StoryNarrationAudioEntity a " +
            "WHERE tm.masterStoryId = c.id AND LOWER(tm.language) = LOWER(c.language) " +
            "AND a.translationId = tm.id AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0" +
            ") OR (c.audioFileUrl IS NOT NULL AND LENGTH(c.audioFileUrl) > 0))"
    )
    fun findByLanguageAndMasterNarrationApprovedWithMasterAudioAndTheme(
        @Param("language") language: String,
        @Param("theme") theme: String,
        pageable: Pageable
    ): Page<StoryTranslationEntity>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language AND c.narrationApprovedAt IS NOT NULL " +
            "AND (LOWER(COALESCE(c.category, '')) LIKE LOWER(CONCAT(:prefix, '%')) OR LOWER(c.theme) LIKE LOWER(CONCAT(:prefix, '%'))) " +
            "AND EXISTS (" +
            "SELECT 1 FROM StoryNarrationAudioEntity a WHERE a.translationId = t.id " +
            "AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0)"
    )
    fun findByLanguageAndMasterNarrationApprovedAndLearnPrefix(
        @Param("language") language: String,
        @Param("prefix") prefix: String,
        pageable: Pageable
    ): Page<StoryTranslationEntity>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language AND c.narrationApprovedAt IS NOT NULL " +
            "AND (LOWER(COALESCE(c.category, '')) LIKE LOWER(CONCAT(:prefix, '%')) OR LOWER(c.theme) LIKE LOWER(CONCAT(:prefix, '%'))) " +
            "AND (" +
            "EXISTS (" +
            "SELECT 1 FROM StoryTranslationEntity tm, StoryNarrationAudioEntity a " +
            "WHERE tm.masterStoryId = c.id AND LOWER(tm.language) = LOWER(c.language) " +
            "AND a.translationId = tm.id AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0" +
            ") OR (c.audioFileUrl IS NOT NULL AND LENGTH(c.audioFileUrl) > 0))"
    )
    fun findByLanguageAndMasterNarrationApprovedWithMasterAudioAndLearnPrefix(
        @Param("language") language: String,
        @Param("prefix") prefix: String,
        pageable: Pageable
    ): Page<StoryTranslationEntity>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.language = :language"
    )
    fun findListingByLanguage(
        @Param("language") language: String,
        pageable: Pageable
    ): Page<StoryTranslationListingProjection>

    @Query(
        "SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM StoryTranslationEntity t, LibraryStoryEntity c " +
            "WHERE c.id = t.masterStoryId AND c.deletedAt IS NULL AND t.masterStoryId = :masterStoryId " +
            "AND t.language = :language"
    )
    fun existsByMasterStoryIdAndLanguage(
        @Param("masterStoryId") masterStoryId: Long,
        @Param("language") language: String
    ): Boolean

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.masterStoryId = :masterStoryId"
    )
    fun findByMasterStoryId(@Param("masterStoryId") masterStoryId: Long): List<StoryTranslationEntity>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.status IN :statuses"
    )
    fun findByStatusIn(@Param("statuses") statuses: List<TranslationPipelineStatus>): List<StoryTranslationEntity>

    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND t.status IN :statuses AND t.retryCount < :maxRetries ORDER BY t.id"
    )
    fun findRetryableByStatusIn(
        @Param("statuses") statuses: List<TranslationPipelineStatus>,
        @Param("maxRetries") maxRetries: Int,
        pageable: Pageable
    ): Page<StoryTranslationEntity>

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

    /**
     * After reject / request-changes: all languages back to a clean pipeline state (content rows kept).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(
        "UPDATE StoryTranslationEntity e SET e.status = :pending, e.lastError = null, e.retryCount = 0, " +
            "e.narrationApprovedAt = null WHERE e.masterStoryId = :masterStoryId"
    )
    fun resetPipelineStateForMasterStory(
        @Param("masterStoryId") masterStoryId: Long,
        @Param("pending") pending: TranslationPipelineStatus
    ): Int

    fun deleteByMasterStoryId(masterStoryId: Long)

    /**
     * Search translations by content (title, content, moral) for parent-facing search.
     * Only returns translations where master story is PUBLISHED.
     * Narration approval checked on master story level.
     */
    @Query(
        "SELECT t FROM StoryTranslationEntity t, LibraryStoryEntity c WHERE c.id = t.masterStoryId " +
            "AND c.deletedAt IS NULL AND c.status = 'PUBLISHED' " +
            "AND t.language = :language " +
            "AND (LOWER(COALESCE(t.title, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(COALESCE(t.content, '')) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(COALESCE(t.moral, '')) LIKE LOWER(CONCAT('%', :q, '%')))"
    )
    fun searchByContent(
        @Param("q") query: String,
        @Param("language") language: String,
        pageable: Pageable
    ): Page<StoryTranslationEntity>
}
