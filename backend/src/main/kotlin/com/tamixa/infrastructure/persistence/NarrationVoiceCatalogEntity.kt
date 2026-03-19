package com.tamixa.infrastructure.persistence

import com.tamixa.domain.narration.NarrationVoiceCatalog
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "narration_voice_catalog")
data class NarrationVoiceCatalogEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    @Column(nullable = false) val provider: String = "",
    @Column(nullable = false) val language: String = "",
    @Column(name = "voice_name", nullable = false) val voiceName: String = "",
    @Column(name = "tone_mode", nullable = false) val toneMode: String = "",
    @Column(name = "is_premium", nullable = false) val isPremium: Boolean = false,
    @Column(name = "is_active", nullable = false) val isActive: Boolean = true
) {
    fun toDomain(): NarrationVoiceCatalog = NarrationVoiceCatalog(
        id = id,
        provider = provider,
        language = language,
        voiceName = voiceName,
        toneMode = toneMode,
        isPremium = isPremium,
        isActive = isActive
    )
}
