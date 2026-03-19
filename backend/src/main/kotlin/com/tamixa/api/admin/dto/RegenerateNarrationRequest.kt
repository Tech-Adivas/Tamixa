package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Size

/** Request for regenerate narration: TTS-only for selected languages. Empty = all. */
data class RegenerateNarrationRequest(
    @field:Size(max = 20)
    val languages: List<String>? = null
)
