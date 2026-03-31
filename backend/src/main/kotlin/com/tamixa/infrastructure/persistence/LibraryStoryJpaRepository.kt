package com.tamixa.infrastructure.persistence

import java.time.Instant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LibraryStoryJpaRepository : JpaRepository<LibraryStoryEntity, Long> {

    @Modifying
    @Query("UPDATE LibraryStoryEntity c SET c.audioFileUrl = null WHERE c.audioFileUrl IS NOT NULL AND c.deletedAt IS NULL")
    fun clearAllAudioUrls(): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "UPDATE LibraryStoryEntity l SET l.status = :status, l.updatedAt = :now " +
            "WHERE l.id IN :ids AND l.deletedAt IS NULL"
    )
    fun bulkUpdateStatusForActiveIds(
        @Param("ids") ids: List<Long>,
        @Param("status") status: String,
        @Param("now") now: Instant
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "UPDATE LibraryStoryEntity l SET l.theme = :theme, l.updatedAt = :now " +
            "WHERE l.id IN :ids AND l.deletedAt IS NULL"
    )
    fun bulkUpdateThemeForActiveIds(
        @Param("ids") ids: List<Long>,
        @Param("theme") theme: String,
        @Param("now") now: Instant
    ): Int

    @Query("SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.deletedAt IS NULL")
    fun findByLanguage(@Param("language") language: String, pageable: Pageable): Page<LibraryStoryEntity>

    @Query("SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.deletedAt IS NULL")
    fun findListingByLanguage(@Param("language") language: String, pageable: Pageable): Page<LibraryStoryListingProjection>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.narrationApprovedAt IS NOT NULL AND c.deletedAt IS NULL"
    )
    fun findListingByLanguageAndNarrationApprovedAtNotNull(
        @Param("language") language: String,
        pageable: Pageable
    ): Page<LibraryStoryListingProjection>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.narrationApprovedAt IS NOT NULL " +
            "AND LOWER(c.theme) = LOWER(:theme) AND c.deletedAt IS NULL"
    )
    fun findListingByLanguageAndNarrationApprovedAndTheme(
        @Param("language") language: String,
        @Param("theme") theme: String,
        pageable: Pageable
    ): Page<LibraryStoryListingProjection>

    @Query(
        "SELECT DISTINCT c.theme FROM LibraryStoryEntity c WHERE c.language = :language AND c.narrationApprovedAt IS NOT NULL " +
            "AND c.deletedAt IS NULL " +
            "AND ((c.audioFileUrl IS NOT NULL AND LENGTH(c.audioFileUrl) > 0) OR EXISTS (" +
            "SELECT 1 FROM StoryNarrationAudioEntity a, StoryTranslationEntity t " +
            "WHERE a.translationId = t.id AND t.masterStoryId = c.id AND t.language = :language " +
            "AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0)) " +
            "ORDER BY c.theme"
    )
    fun findDistinctThemesByNarrationApproved(@Param("language") language: String): List<String>

    @Query(
        "SELECT DISTINCT c.theme FROM LibraryStoryEntity c " +
            "JOIN StoryTranslationEntity t ON t.masterStoryId = c.id AND t.language = :language " +
            "WHERE c.narrationApprovedAt IS NOT NULL AND c.deletedAt IS NULL AND EXISTS (" +
            "SELECT 1 FROM StoryNarrationAudioEntity a WHERE a.translationId = t.id " +
            "AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0) " +
            "ORDER BY c.theme"
    )
    fun findDistinctThemesByNarrationApprovedAndTranslationLanguage(@Param("language") language: String): List<String>

    /**
     * Non-master locale [language] with approved master story, where **playable** audio is on the master translation
     * (or legacy `audio_file_url`) — same rule as [StoryTranslationJpaRepository.findByLanguageAndMasterNarrationApprovedWithMasterAudio].
     */
    @Query(
        "SELECT DISTINCT c.theme FROM LibraryStoryEntity c " +
            "JOIN StoryTranslationEntity t ON t.masterStoryId = c.id AND t.language = :language " +
            "WHERE c.narrationApprovedAt IS NOT NULL AND c.deletedAt IS NULL AND (" +
            "EXISTS (" +
            "SELECT 1 FROM StoryTranslationEntity tm, StoryNarrationAudioEntity a " +
            "WHERE tm.masterStoryId = c.id AND LOWER(tm.language) = LOWER(c.language) " +
            "AND a.translationId = tm.id AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0" +
            ") OR (c.audioFileUrl IS NOT NULL AND LENGTH(c.audioFileUrl) > 0)) " +
            "ORDER BY c.theme"
    )
    fun findDistinctThemesByNarrationApprovedAndTranslationLanguageWithMasterAudio(
        @Param("language") language: String
    ): List<String>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.narrationApprovedAt IS NOT NULL " +
            "AND c.deletedAt IS NULL " +
            "AND ((c.audioFileUrl IS NOT NULL AND LENGTH(c.audioFileUrl) > 0) OR EXISTS (" +
            "SELECT 1 FROM StoryNarrationAudioEntity a, StoryTranslationEntity t " +
            "WHERE a.translationId = t.id AND t.masterStoryId = c.id AND t.language = :language " +
            "AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0))"
    )
    fun findByLanguageAndNarrationApprovedAtNotNull(
        @Param("language") language: String,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.narrationApprovedAt IS NOT NULL " +
            "AND LOWER(c.theme) = LOWER(:theme) AND c.deletedAt IS NULL " +
            "AND ((c.audioFileUrl IS NOT NULL AND LENGTH(c.audioFileUrl) > 0) OR EXISTS (" +
            "SELECT 1 FROM StoryNarrationAudioEntity a, StoryTranslationEntity t " +
            "WHERE a.translationId = t.id AND t.masterStoryId = c.id AND t.language = :language " +
            "AND a.voiceProfile = 'default' AND a.status = 'READY' AND LENGTH(a.audioUrl) > 0))"
    )
    fun findByLanguageAndNarrationApprovedAtNotNullAndTheme(
        @Param("language") language: String,
        @Param("theme") theme: String,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query("SELECT c FROM LibraryStoryEntity c WHERE c.id IN :ids AND c.deletedAt IS NULL")
    fun findListingByIdIn(@Param("ids") ids: List<Long>): List<LibraryStoryListingProjection>

    @Query("SELECT c FROM LibraryStoryEntity c WHERE c.age BETWEEN :minAge AND :maxAge AND c.deletedAt IS NULL")
    fun findByAgeBetween(
        @Param("minAge") minAge: Int,
        @Param("maxAge") maxAge: Int,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query("SELECT c FROM LibraryStoryEntity c WHERE c.status = :status AND c.deletedAt IS NULL")
    fun findByStatus(@Param("status") status: String, pageable: Pageable): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.status = :status AND c.narrationApprovedAt IS NOT NULL AND c.deletedAt IS NULL"
    )
    fun findByStatusAndNarrationApprovedAtIsNotNull(
        @Param("status") status: String,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.status = :status AND c.narrationApprovedAt IS NULL AND c.deletedAt IS NULL"
    )
    fun findByStatusAndNarrationApprovedAtIsNull(
        @Param("status") status: String,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query("SELECT c FROM LibraryStoryEntity c WHERE c.status IN :statuses AND c.deletedAt IS NULL")
    fun findByStatusIn(@Param("statuses") statuses: List<String>, pageable: Pageable): Page<LibraryStoryEntity>

    @Query("SELECT c FROM LibraryStoryEntity c WHERE c.deletedAt IS NULL ORDER BY c.id DESC")
    fun findAllActiveByIdDesc(pageable: Pageable): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.deletedAt IS NULL " +
            "ORDER BY CASE WHEN c.status = 'PROCESSING' THEN 0 ELSE 1 END, c.id DESC"
    )
    fun findAllWithProcessingFirst(pageable: Pageable): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.deletedAt IS NULL AND c.narrationApprovedAt IS NOT NULL " +
            "ORDER BY CASE WHEN c.status = 'PROCESSING' THEN 0 ELSE 1 END, c.id DESC"
    )
    fun findAllWithProcessingFirstAndNarrationApprovedAtNotNull(pageable: Pageable): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.deletedAt IS NULL AND c.narrationApprovedAt IS NULL " +
            "ORDER BY CASE WHEN c.status = 'PROCESSING' THEN 0 ELSE 1 END, c.id DESC"
    )
    fun findAllWithProcessingFirstAndNarrationApprovedAtNull(pageable: Pageable): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.deletedAt IS NULL AND c.status IN :statuses " +
            "ORDER BY CASE WHEN c.status = 'PROCESSING' THEN 0 ELSE 1 END, c.id DESC"
    )
    fun findByStatusInWithProcessingFirst(
        @Param("statuses") statuses: List<String>,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.deletedAt IS NULL AND c.status IN :statuses AND c.narrationApprovedAt IS NOT NULL " +
            "ORDER BY CASE WHEN c.status = 'PROCESSING' THEN 0 ELSE 1 END, c.id DESC"
    )
    fun findByStatusInWithProcessingFirstAndNarrationApprovedAtNotNull(
        @Param("statuses") statuses: List<String>,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.deletedAt IS NULL AND c.status IN :statuses AND c.narrationApprovedAt IS NULL " +
            "ORDER BY CASE WHEN c.status = 'PROCESSING' THEN 0 ELSE 1 END, c.id DESC"
    )
    fun findByStatusInWithProcessingFirstAndNarrationApprovedAtNull(
        @Param("statuses") statuses: List<String>,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.deletedAt IS NULL AND c.status IN ('PUBLISHED', 'PROCESSING', 'READY') " +
            "AND c.narrationApprovedAt IS NULL ORDER BY c.id DESC"
    )
    fun findByStatusPublishedAndNarrationApprovedAtNull(pageable: Pageable): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.deletedAt IS NULL AND c.narrationApprovedAt IS NOT NULL ORDER BY c.id DESC"
    )
    fun findByNarrationApprovedAtNotNull(pageable: Pageable): Page<LibraryStoryEntity>

    @Query(
        "SELECT COUNT(c) FROM LibraryStoryEntity c " +
            "WHERE LOWER(c.title) = LOWER(:title) AND c.id <> :id AND c.deletedAt IS NULL"
    )
    fun countByTitleIgnoreCaseActiveAndIdNot(@Param("title") title: String, @Param("id") id: Long): Long

    @Query(
        "SELECT COUNT(c) FROM LibraryStoryEntity c WHERE LOWER(c.title) = LOWER(:title) AND c.deletedAt IS NULL"
    )
    fun countByTitleIgnoreCaseActive(@Param("title") title: String): Long

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.deletedAt IS NULL " +
            "AND (LOWER(c.theme) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR (c.title IS NOT NULL AND LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))))"
    )
    fun searchByThemeOrTitle(
        @Param("q") query: String,
        @Param("language") language: String,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query("SELECT c FROM LibraryStoryEntity c WHERE c.deletedAt IS NOT NULL ORDER BY c.deletedAt DESC")
    fun findSoftDeleted(pageable: Pageable): Page<LibraryStoryEntity>

    @Query("SELECT c.id FROM LibraryStoryEntity c WHERE c.deletedAt IS NOT NULL AND c.deletedAt < :cutoff ORDER BY c.deletedAt ASC")
    fun findIdsSoftDeletedBefore(@Param("cutoff") cutoff: Instant): List<Long>
}
