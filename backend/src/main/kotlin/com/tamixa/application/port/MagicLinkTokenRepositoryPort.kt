package com.tamixa.application.port

import java.time.Instant

/**
 * Port for magic link / passwordless email code tokens.
 */
interface MagicLinkTokenRepositoryPort {

    fun save(email: String, token: String, expiresAt: Instant, shortCode: String): Long

    fun findValidByEmailAndCode(email: String, shortCode: String): MagicLinkToken?

    fun markUsed(id: Long)
}

data class MagicLinkToken(
    val id: Long,
    val email: String,
    val token: String,
    val expiresAt: Instant,
    val shortCode: String
)
