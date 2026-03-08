package com.araro.domain

import java.time.Instant

/**
 * Device fingerprint record for fraud prevention.
 * Financial safety: limit free trial eligibility to one device per account.
 */
data class Device(
    val id: Long,
    val parentId: Long,
    val deviceHash: String,
    val createdAt: Instant
)
