package com.tamixa.api.admin.dto

import java.time.Instant

data class ParentDetailDto(
    val id: Long,
    val email: String,
    val role: String,
    val status: String,
    val plan: String,
    val phone: String?,
    val createdAt: Instant,
    val suspendedAt: Instant?
)
