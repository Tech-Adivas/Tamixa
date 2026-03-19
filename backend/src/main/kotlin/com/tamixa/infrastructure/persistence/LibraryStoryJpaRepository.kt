package com.tamixa.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LibraryStoryJpaRepository : JpaRepository<LibraryStoryEntity, Long> {

    @Modifying
    @Query("UPDATE LibraryStoryEntity c SET c.audioFileUrl = null WHERE c.audioFileUrl IS NOT NULL")
    fun clearAllAudioUrls(): Int

    fun findByLanguage(language: String, pageable: Pageable): Page<LibraryStoryEntity>

    fun findListingByLanguage(language: String, pageable: Pageable): Page<LibraryStoryListingProjection>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.narrationApprovedAt IS NOT NULL"
    )
    fun findListingByLanguageAndNarrationApprovedAtNotNull(
        @Param("language") language: String,
        pageable: Pageable
    ): Page<LibraryStoryListingProjection>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.narrationApprovedAt IS NOT NULL AND LOWER(c.theme) = LOWER(:theme)"
    )
    fun findListingByLanguageAndNarrationApprovedAndTheme(
        @Param("language") language: String,
        @Param("theme") theme: String,
        pageable: Pageable
    ): Page<LibraryStoryListingProjection>

    @Query(
        "SELECT DISTINCT c.theme FROM LibraryStoryEntity c WHERE c.language = :language AND c.narrationApprovedAt IS NOT NULL ORDER BY c.theme"
    )
    fun findDistinctThemesByNarrationApproved(@Param("language") language: String): List<String>

    /**
     * Distinct themes for masters that have approved translation in the given language.
     * Used when language != ta (masters are Tamil; translations provide other languages).
     */
    @Query(
        "SELECT DISTINCT c.theme FROM LibraryStoryEntity c " +
            "JOIN StoryTranslationEntity t ON t.masterStoryId = c.id AND t.language = :language " +
            "WHERE c.narrationApprovedAt IS NOT NULL ORDER BY c.theme"
    )
    fun findDistinctThemesByNarrationApprovedAndTranslationLanguage(@Param("language") language: String): List<String>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.narrationApprovedAt IS NOT NULL"
    )
    fun findByLanguageAndNarrationApprovedAtNotNull(
        @Param("language") language: String,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.language = :language AND c.narrationApprovedAt IS NOT NULL AND LOWER(c.theme) = LOWER(:theme)"
    )
    fun findByLanguageAndNarrationApprovedAtNotNullAndTheme(
        @Param("language") language: String,
        @Param("theme") theme: String,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    @Query("SELECT c FROM LibraryStoryEntity c WHERE c.id IN :ids")
    fun findListingByIdIn(ids: List<Long>): List<LibraryStoryListingProjection>

    fun findByAgeBetween(minAge: Int, maxAge: Int, pageable: Pageable): Page<LibraryStoryEntity>

    fun findByStatus(status: String, pageable: Pageable): Page<LibraryStoryEntity>

    fun findByStatusIn(statuses: List<String>, pageable: Pageable): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c ORDER BY CASE WHEN c.status = 'PROCESSING' THEN 0 ELSE 1 END, c.id DESC"
    )
    fun findAllWithProcessingFirst(pageable: Pageable): Page<LibraryStoryEntity>

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.status IN :statuses " +
            "ORDER BY CASE WHEN c.status = 'PROCESSING' THEN 0 ELSE 1 END, c.id DESC"
    )
    fun findByStatusInWithProcessingFirst(
        @Param("statuses") statuses: List<String>,
        pageable: Pageable
    ): Page<LibraryStoryEntity>

    /**
     * Stories pending narration review: PUBLISHED (queued), PROCESSING (pipeline running), READY (pipeline done, awaiting approve).
     * Excludes DRAFT, CHANGES_REQUESTED, REJECTED, and approved stories (narrationApprovedAt not null).
     * Ordered by id DESC so newest (recently bulk-created) stories appear first.
     */
    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.status IN ('PUBLISHED', 'PROCESSING', 'READY') " +
            "AND c.narrationApprovedAt IS NULL ORDER BY c.id DESC"
    )
    fun findByStatusPublishedAndNarrationApprovedAtNull(pageable: Pageable): Page<LibraryStoryEntity>

    /** Approved stories (content approved) for "Story to Speech": trigger TTS pipeline from here. */
    @Query("SELECT c FROM LibraryStoryEntity c WHERE c.narrationApprovedAt IS NOT NULL ORDER BY c.id DESC")
    fun findByNarrationApprovedAtNotNull(pageable: Pageable): Page<LibraryStoryEntity>

    fun existsByTitleIgnoreCaseAndIdNot(title: String, id: Long): Boolean

    fun existsByTitleIgnoreCase(title: String): Boolean

    @Query(
        "SELECT c FROM LibraryStoryEntity c WHERE c.language = :language " +
            "AND (LOWER(c.theme) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR (c.title IS NOT NULL AND LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))))"
    )
    fun searchByThemeOrTitle(@Param("q") query: String, @Param("language") language: String, pageable: Pageable): Page<LibraryStoryEntity>
}
