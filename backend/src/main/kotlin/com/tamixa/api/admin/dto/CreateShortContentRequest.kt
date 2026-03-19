package com.tamixa.api.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateShortContentRequest(
    @field:NotBlank(message = "type is required")
    @field:Size(max = 50)
    val type: String,

    @field:NotBlank(message = "content is required")
    val content: String,

    val answer: String? = null,

    @field:NotBlank(message = "language is required")
    @field:Size(max = 10)
    val language: String = "ta",

    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val displayDate: java.time.LocalDate? = null,
    val audioUrl: String? = null,

    @field:Size(max = 20)
    val status: String = "DRAFT"
)
