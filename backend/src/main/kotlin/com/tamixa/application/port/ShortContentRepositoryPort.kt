package com.tamixa.application.port

import com.tamixa.domain.ShortContent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface ShortContentRepositoryPort {

    fun save(shortContent: ShortContent): ShortContent

    fun findById(id: Long): ShortContent?

    fun findByTypeAndLanguageAndStatus(type: String, language: String, status: String, pageable: Pageable): Page<ShortContent>

    fun findByDisplayDateAndTypeAndLanguageAndStatus(
        displayDate: LocalDate,
        type: String,
        language: String,
        status: String
    ): ShortContent?

    fun findByLanguageAndStatus(language: String, status: String, pageable: Pageable): Page<ShortContent>

    fun findByTypeAndLanguageAndStatusOrderByDisplayDateDesc(
        type: String,
        language: String,
        status: String,
        pageable: Pageable
    ): Page<ShortContent>

    fun findForAdmin(type: String?, language: String?, status: String?, pageable: Pageable): Page<ShortContent>

    fun deleteById(id: Long)
}
