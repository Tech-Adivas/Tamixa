package com.tamixa.api.auth.dto

import jakarta.validation.constraints.NotNull

data class StoryArtPersonalizationRequest(
    @field:NotNull
    val optIn: Boolean
)
