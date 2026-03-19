package com.tamixa.api.narration.dto

import com.tamixa.domain.narration.ToneMode
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * API request to trigger narration generation for a translation.
 * toneMode defaults to CALM; voiceProfiles default to ["default"].
 * Premium voices require subscription.voicePremium.
 */
data class NarrationRequestDto(
    @field:NotNull @field:Positive val translationId: Long,
    val toneMode: ToneMode = ToneMode.CALM,
    val voiceProfiles: List<String> = listOf("default")
) {
    init {
        require(voiceProfiles.isNotEmpty()) { "voiceProfiles cannot be empty" }
    }
}
