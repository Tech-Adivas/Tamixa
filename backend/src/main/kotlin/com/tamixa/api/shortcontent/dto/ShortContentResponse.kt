package com.tamixa.api.shortcontent.dto

import java.time.Instant
import java.time.LocalDate

data class ShortContentResponse(
    val id: Long,
    val type: String,
    val content: String,
    val answer: String?,
    val language: String,
    val ageMin: Int?,
    val ageMax: Int?,
    val displayDate: LocalDate?,
    val audioUrl: String?,
    val createdAt: Instant
)
