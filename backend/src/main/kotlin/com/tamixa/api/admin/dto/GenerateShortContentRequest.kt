package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class GenerateShortContentRequest(
    @field:NotBlank(message = "type is required")
    @field:Size(max = 50)
    val type: String,

    @field:NotBlank(message = "language is required")
    @field:Size(max = 10)
    val language: String = "ta",

    @field:Min(1)
    @field:Max(20)
    val count: Int = 5
)
