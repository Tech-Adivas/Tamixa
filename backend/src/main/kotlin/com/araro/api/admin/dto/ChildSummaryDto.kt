package com.araro.api.admin.dto

import java.time.Instant
import java.time.LocalDate

data class ChildSummaryDto(
    val id: Long,
    val parentId: Long,
    val parentEmail: String?,
    val name: String,
    val dateOfBirth: LocalDate,
    val age: Int,
    val languagePreference: String?,
    val createdAt: Instant
)
