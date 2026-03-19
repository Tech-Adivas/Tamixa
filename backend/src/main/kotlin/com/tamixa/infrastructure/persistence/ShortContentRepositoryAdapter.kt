package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.ShortContentRepositoryPort
import com.tamixa.domain.ShortContent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class ShortContentRepositoryAdapter(
    private val jpaRepository: ShortContentJpaRepository
) : ShortContentRepositoryPort {

    private fun toDomain(e: ShortContentEntity): ShortContent = ShortContent(
        id = e.id,
        type = e.type,
        content = e.content,
        answer = e.answer,
        language = e.language,
        ageMin = e.ageMin,
        ageMax = e.ageMax,
        displayDate = e.displayDate,
        audioUrl = e.audioUrl,
        status = e.status,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt
    )

    private fun toEntity(d: ShortContent): ShortContentEntity = ShortContentEntity(
        id = d.id,
        type = d.type,
        content = d.content,
        answer = d.answer,
        language = d.language,
        ageMin = d.ageMin,
        ageMax = d.ageMax,
        displayDate = d.displayDate,
        audioUrl = d.audioUrl,
        status = d.status,
        createdAt = d.createdAt,
        updatedAt = d.updatedAt
    )

    @Transactional
    override fun save(shortContent: ShortContent): ShortContent {
        val entity = toEntity(shortContent).apply { updatedAt = java.time.Instant.now() }
        val saved = jpaRepository.save(entity)
        return toDomain(saved)
    }

    override fun findById(id: Long): ShortContent? =
        jpaRepository.findById(id).map { toDomain(it) }.orElse(null)

    override fun findByTypeAndLanguageAndStatus(
        type: String,
        language: String,
        status: String,
        pageable: Pageable
    ): Page<ShortContent> =
        jpaRepository.findByTypeAndLanguageAndStatus(type, language, status, pageable).map { toDomain(it) }

    override fun findByDisplayDateAndTypeAndLanguageAndStatus(
        displayDate: LocalDate,
        type: String,
        language: String,
        status: String
    ): ShortContent? =
        jpaRepository.findByDisplayDateAndTypeAndLanguageAndStatus(displayDate, type, language, status)?.let { toDomain(it) }

    override fun findByLanguageAndStatus(language: String, status: String, pageable: Pageable): Page<ShortContent> =
        jpaRepository.findByLanguageAndStatus(language, status, pageable).map { toDomain(it) }

    override fun findByTypeAndLanguageAndStatusOrderByDisplayDateDesc(
        type: String,
        language: String,
        status: String,
        pageable: Pageable
    ): Page<ShortContent> =
        jpaRepository.findByTypeAndLanguageAndStatusOrderByDisplayDateDesc(type, language, status, pageable).map { toDomain(it) }

    override fun findForAdmin(type: String?, language: String?, status: String?, pageable: Pageable): Page<ShortContent> =
        jpaRepository.findForAdmin(type, language, status, pageable).map { toDomain(it) }

    @Transactional
    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }
}
