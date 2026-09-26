package com.tamixa.api.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class InteractiveSegmentScriptsFillRequest(
    @field:Size(max = 10)
    val language: String = "ta",
    @field:NotBlank
    @field:Size(max = 100_000)
    val interactiveGraph: String,
    @field:Size(max = 30_000)
    val storyText: String? = null,
)
