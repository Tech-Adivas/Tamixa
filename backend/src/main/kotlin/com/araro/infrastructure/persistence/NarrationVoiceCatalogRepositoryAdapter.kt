package com.araro.infrastructure.persistence

import com.araro.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.araro.domain.narration.NarrationVoiceCatalog
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
