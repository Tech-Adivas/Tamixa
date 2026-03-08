package com.araro.api.playback.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class SavePositionRequest(
    @field:NotNull val storyId: Long,
    @field:NotNull val storySource: String,
    @field:Min(0) @field:Max(86400) val positionSeconds: Int,
    val childId: Long? = null
)
