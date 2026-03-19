package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Size

data class FlagStoryRequest(
    @field:Size(max = 1000)
    val reason: String? = null
)
