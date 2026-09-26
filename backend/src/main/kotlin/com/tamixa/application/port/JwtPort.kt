package com.tamixa.application.port

import java.time.Instant

data class TokenClaims(
    val email: String,
    val role: String,
    val issuedAt: Instant
)

interface JwtPort {

    fun generateAccessToken(email: String, role: String): String

    fun generateRefreshToken(email: String, role: String): String

    fun validateAccessToken(token: String): TokenClaims?

    fun validateRefreshToken(token: String): TokenClaims?

    fun getAccessExpirationSeconds(): Long

    /**
     * Extract token expiration time. Used for setting TTL on revoked tokens.
     */
    fun getTokenExpiration(token: String): Instant?
}
