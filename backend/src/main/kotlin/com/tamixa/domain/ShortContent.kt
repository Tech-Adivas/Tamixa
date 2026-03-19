package com.tamixa.domain

import java.time.Instant
import java.time.LocalDate

data class ShortContent(
    val id: Long,
    val type: String,
    val content: String,
    val answer: String?,
    val language: String,
    val ageMin: Int?,
    val ageMax: Int?,
    val displayDate: LocalDate?,
    val audioUrl: String?,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
