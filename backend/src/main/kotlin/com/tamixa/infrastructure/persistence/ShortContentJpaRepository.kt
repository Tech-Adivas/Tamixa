package com.tamixa.infrastructure.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface ShortContentJpaRepository : JpaRepository<ShortContentEntity, Long> {

    @Query(
        "SELECT s FROM ShortContentEntity s WHERE " +
        "(:type IS NULL OR :type = '' OR s.type = :type) AND " +
        "(:language IS NULL OR :language = '' OR s.language = :language) AND " +
        "(:status IS NULL OR :status = '' OR s.status = :status) " +
        "ORDER BY s.createdAt DESC"
    )
    fun findForAdmin(
        @Param("type") type: String?,
        @Param("language") language: String?,
        @Param("status") status: String?,
        pageable: Pageable
    ): Page<ShortContentEntity>

    fun findByTypeAndLanguageAndStatus(type: String, language: String, status: String, pageable: Pageable): Page<ShortContentEntity>

    fun findByDisplayDateAndTypeAndLanguageAndStatus(
        displayDate: LocalDate,
        type: String,
        language: String,
        status: String
    ): ShortContentEntity?

    fun findByLanguageAndStatus(language: String, status: String, pageable: Pageable): Page<ShortContentEntity>

    fun findByTypeAndLanguageAndStatusOrderByDisplayDateDesc(
        type: String,
        language: String,
        status: String,
        pageable: Pageable
    ): Page<ShortContentEntity>
}
