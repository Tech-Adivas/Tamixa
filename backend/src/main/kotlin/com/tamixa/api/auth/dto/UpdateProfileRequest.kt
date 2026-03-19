package com.tamixa.api.auth.dto

import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:Size(max = 100)
    val nickname: String? = null,
    @field:Size(max = 100)
    val displayName: String? = null
)
