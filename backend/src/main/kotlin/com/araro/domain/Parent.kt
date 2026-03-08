package com.araro.domain

import java.time.Instant

data class Parent(
    val id: Long,
    val email: String,
    val passwordHash: String,
    val role: Role,
    val createdAt: Instant,
    val phone: String? = null,
    val suspendedAt: Instant? = null
)
