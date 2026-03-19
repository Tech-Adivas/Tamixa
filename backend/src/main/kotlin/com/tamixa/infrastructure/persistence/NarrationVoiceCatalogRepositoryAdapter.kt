package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.tamixa.domain.narration.NarrationVoiceCatalog
import org.springframework.stereotype.Component

@Component
class NarrationVoiceCatalogRepositoryAdapter(
    private val jpaRepository: NarrationVoiceCatalogJpaRepository
) : NarrationVoiceCatalogRepositoryPort {

    override fun findByToneMode(toneMode: String): NarrationVoiceCatalog? =
        jpaRepository.findFirstByToneModeAndIsActiveTrue(toneMode)?.toDomain()

    override fun findByToneModeAndLanguage(toneMode: String, language: String): NarrationVoiceCatalog? =
        jpaRepository.findByToneModeAndLanguageAndIsActiveTrue(toneMode, language)?.toDomain()

    override fun findAllActive(): List<NarrationVoiceCatalog> =
        jpaRepository.findAllByIsActiveTrue().map { it.toDomain() }
}
