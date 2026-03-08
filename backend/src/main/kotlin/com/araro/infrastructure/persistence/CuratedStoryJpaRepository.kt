package com.araro.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CuratedStoryJpaRepository : JpaRepository<CuratedStoryEntity, Long> {

    @Modifying
    @Query("UPDATE CuratedStoryEntity c SET c.audioFileUrl = null WHERE c.audioFileUrl IS NOT NULL")
    fun clearAllAudioUrls(): Int

    fun findByLanguage(language: String, pageable: Pageable): Page<CuratedStoryEntity>

    fun findListingByLanguage(language: String, pageable: Pageable): Page<CuratedStoryListingProjection>

    @Query("SELECT c FROM CuratedStoryEntity c WHERE c.id IN :ids")
    fun findListingByIdIn(ids: List<Long>): List<CuratedStoryListingProjection>

    fun findByAgeBetween(minAge: Int, maxAge: Int, pageable: Pageable): Page<CuratedStoryEntity>

    fun findByStatus(status: String, pageable: Pageable): Page<CuratedStoryEntity>

    fun findByStatusIn(statuses: List<String>, pageable: Pageable): Page<CuratedStoryEntity>

    @Query(
        "SELECT c FROM CuratedStoryEntity c ORDER BY CASE WHEN c.status = 'PROCESSING' THEN 0 ELSE 1 END, c.id DESC"
    )
    fun findAllWithProcessingFirst(pageable: Pageable): Page<CuratedStoryEntity>

    @Query(
        "SELECT c FROM CuratedStoryEntity c WHERE c.status IN :statuses " +
            "ORDER BY CASE WHEN c.status = 'PROCESSING' THEN 0 ELSE 1 END, c.id DESC"
    )
    fun findByStatusInWithProcessingFirst(
        @Param("statuses") statuses: List<String>,
        pageable: Pageable
    ): Page<CuratedStoryEntity>

    fun existsByTitleIgnoreCaseAndIdNot(title: String, id: Long): Boolean

    fun existsByTitleIgnoreCase(title: String): Boolean

    @Query(
        "SELECT c FROM CuratedStoryEntity c WHERE c.language = :language " +
            "AND (LOWER(c.theme) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR (c.title IS NOT NULL AND LOWER(c.title) LIKE LOWER(CONCAT('%', :q, '%'))))"
    )
    fun searchByThemeOrTitle(@Param("q") query: String, @Param("language") language: String, pageable: Pageable): Page<CuratedStoryEntity>
}
