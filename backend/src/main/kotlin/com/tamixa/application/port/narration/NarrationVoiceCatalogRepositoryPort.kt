package com.tamixa.application.port.narration

import com.tamixa.domain.narration.NarrationVoiceCatalog

/**
 * TTS voice catalog for premium gating.
 * toneMode (e.g. default, calm) maps to voice_profile in story_narration_audio.
 */
interface NarrationVoiceCatalogRepositoryPort {
    fun findByToneMode(toneMode: String): NarrationVoiceCatalog?
    fun findByToneModeAndLanguage(toneMode: String, language: String): NarrationVoiceCatalog?
    fun findAllActive(): List<NarrationVoiceCatalog>
}
