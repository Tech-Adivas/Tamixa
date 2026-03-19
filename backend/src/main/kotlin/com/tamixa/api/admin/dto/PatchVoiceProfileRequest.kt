package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Size

data class PatchVoiceProfileRequest(
    @field:Size(max = 255)
    val heygenVoiceId: String? = null
)
