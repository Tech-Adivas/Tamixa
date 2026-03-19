package com.tamixa.domain

import java.time.Instant

/**
 * Server-side usage for plan limit enforcement. Never trust client-reported usage.
 */
data class UsageTracking(
    val id: Long,
    val parentId: Long,
    val month: String,
    val storiesGenerated: Int,
    val voiceGenerations: Int,
    val createdAt: Instant,
    val updatedAt: Instant
)
