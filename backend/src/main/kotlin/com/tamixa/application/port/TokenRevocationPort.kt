package com.tamixa.application.port

import java.time.Instant

/**
 * Port for revoking JWT tokens before their natural expiration.
 * Used for logout, account deletion, password changes, and security incidents.
 */
interface TokenRevocationPort {

    /**
     * Revoke a specific token. The token will be stored until its expiration time.
     * @param token The JWT token to revoke (will be hashed before storage)
     * @param expiresAt When the token naturally expires (used for TTL)
     */
    fun revokeToken(token: String, expiresAt: Instant)

    /**
     * Check if a specific token has been revoked.
     * @param token The JWT token to check
     * @return true if the token is revoked, false otherwise
     */
    fun isRevoked(token: String): Boolean

    /**
     * Revoke all tokens for a specific user issued before the given timestamp.
     * Used for account deletion, password changes, or role changes.
     * @param email User's email address
     * @param beforeTimestamp All tokens issued before this time are considered revoked
     */
    fun revokeAllForUser(email: String, beforeTimestamp: Instant = Instant.now())

    /**
     * Check if a user's tokens issued at a specific time should be revoked.
     * @param email User's email address
     * @param issuedAt When the token was issued
     * @return true if tokens issued at this time should be revoked
     */
    fun isUserRevoked(email: String, issuedAt: Instant): Boolean
}
