package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface NarrationVoiceCatalogJpaRepository : JpaRepository<NarrationVoiceCatalogEntity, Long> {
    fun findFirstByToneModeAndIsActiveTrue(toneMode: String): NarrationVoiceCatalogEntity?
    fun findByToneModeAndLanguageAndIsActiveTrue(toneMode: String, language: String): NarrationVoiceCatalogEntity?
    fun findAllByIsActiveTrue(): List<NarrationVoiceCatalogEntity>
}
