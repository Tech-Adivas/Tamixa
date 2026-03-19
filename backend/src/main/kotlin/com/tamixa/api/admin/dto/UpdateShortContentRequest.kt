package com.tamixa.api.admin.dto

import jakarta.validation.constraints.Size

data class UpdateShortContentRequest(
    @field:Size(max = 50)
    val type: String? = null,

    val content: String? = null,
    val answer: String? = null,

    @field:Size(max = 10)
    val language: String? = null,

    val ageMin: Int? = null,
    val ageMax: Int? = null,
    val displayDate: java.time.LocalDate? = null,
    val audioUrl: String? = null,

    @field:Size(max = 20)
    val status: String? = null
)
