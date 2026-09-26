package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Size

data class InteractiveGraphFromStoryRequest(
    @field:Size(max = 10)
    val language: String = "ta",
    @field:Size(max = 30000)
    val storyText: String? = null,
)
