package com.tamixa.api.admin.dto

import java.time.Instant

data class AdminUserDto(
    val id: Long,
    val email: String,
    val role: String,
    val createdAt: Instant
)
