package com.tamixa.domain.narration

/**
 * TTS voice catalog entry for narration.
 * Distinct from VoiceProfile (cloned parent voice)—this is the neural TTS voice catalog.
 *
 * Drives premium monetization: isPremium voices require subscription.voicePremium.
 * Server-enforced gating prevents client bypass.
 */
data class NarrationVoiceCatalog(
    val id: Long,
    val provider: String,
    val language: String,
    val voiceName: String,
    val toneMode: String,
    val isPremium: Boolean,
    val isActive: Boolean
) {
    /** Voice key used in story_narration_audio.voice_profile (e.g. "default", "calm"). */
    fun voiceKey(): String = toneMode.ifBlank { voiceName.lowercase().replace(" ", "_") }
}
