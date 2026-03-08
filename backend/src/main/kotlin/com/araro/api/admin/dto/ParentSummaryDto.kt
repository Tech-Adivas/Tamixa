package com.araro.api.admin.dto

import java.time.Instant

data class ParentSummaryDto(
    val id: Long,
    val email: String,
    val role: String,
    val status: String,
    val plan: String,
    val createdAt: Instant
)
