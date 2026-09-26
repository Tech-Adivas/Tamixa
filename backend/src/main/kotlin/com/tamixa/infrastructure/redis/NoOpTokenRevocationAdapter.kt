package com.tamixa.infrastructure.redis

import com.tamixa.application.port.TokenRevocationPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * No-op implementation of token revocation.
 * Used when JWT revocation is disabled (e.g., in dev environments).
 * All tokens are considered valid until natural expiration.
 */
@Service
@ConditionalOnProperty(name = ["app.jwt.revocation.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpTokenRevocationAdapter : TokenRevocationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        log.warn("JWT token revocation is DISABLED. Tokens cannot be revoked before expiration.")
    }

    override fun revokeToken(token: String, expiresAt: Instant) {
        log.debug("Token revocation skipped (disabled)")
    }

    override fun isRevoked(token: String): Boolean = false

    override fun revokeAllForUser(email: String, beforeTimestamp: Instant) {
        log.debug("User token revocation skipped (disabled) for email={}", email)
    }

    override fun isUserRevoked(email: String, issuedAt: Instant): Boolean = false
}
