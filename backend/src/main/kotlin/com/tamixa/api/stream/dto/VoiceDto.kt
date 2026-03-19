package com.tamixa.api.stream.dto

data class VoiceDto(
    val voiceProfile: String,
    val isPremium: Boolean,
    /** Human-readable label for cloned voices (e.g. "Dharshik voice"). null for default/premium. */
    val displayLabel: String? = null
)
