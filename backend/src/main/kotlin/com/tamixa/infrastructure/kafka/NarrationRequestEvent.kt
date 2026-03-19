package com.tamixa.infrastructure.kafka

import com.tamixa.domain.narration.ToneMode

/**
 * Request to process conversational narration for a translation.
 * Event key for exactly-once: translationId
 *
 * toneMode: CALM (default, bedtime) or EXPRESSIVE. Exposed to API; validated for premium voices.
 * voiceProfiles: List of voice identifiers to generate. "default" = built-in; custom IDs require subscription.
 * parentId: Optional; when set, enables premium voice validation for subscription.voicePremium.
 */
data class NarrationRequestEvent(
    val translationId: Long,
    val toneMode: ToneMode = ToneMode.CALM,
    val voiceProfiles: List<String> = listOf("default"),
    val parentId: Long? = null
)
