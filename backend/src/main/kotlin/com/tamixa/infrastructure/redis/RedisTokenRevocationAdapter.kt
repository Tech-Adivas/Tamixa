package com.tamixa.infrastructure.redis

import com.tamixa.application.port.TokenRevocationPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Redis-backed token revocation store.
 * Tokens are hashed before storage for security.
 * Uses TTL for automatic cleanup when tokens expire naturally.
 */
@Service
@ConditionalOnProperty(name = ["app.jwt.revocation.enabled"], havingValue = "true")
@ConditionalOnBean(RedisTemplate::class)
class RedisTokenRevocationAdapter(
    private val redisTemplate: RedisTemplate<String, String>
) : TokenRevocationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun revokeToken(token: String, expiresAt: Instant) {
        val key = tokenKey(token)
        val ttl = Duration.between(Instant.now(), expiresAt).seconds
        
        if (ttl > 0) {
            try {
                redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS)
                log.debug("Token revoked, TTL={}s", ttl)
            } catch (e: Exception) {
                log.error("Failed to revoke token in Redis: {}", e.message, e)
                throw e
            }
        } else {
            log.debug("Token already expired, skipping revocation")
        }
    }

    override fun isRevoked(token: String): Boolean {
        val key = tokenKey(token)
        return try {
            redisTemplate.hasKey(key) == true
        } catch (e: Exception) {
            log.warn("Redis token revocation check failed, allowing request: {}", e.message)
            // Fail open: if Redis is down, don't block all authentication
            false
        }
    }

    override fun revokeAllForUser(email: String, beforeTimestamp: Instant) {
        val key = userRevocationKey(email)
        try {
            // Store revocation timestamp; tokens issued before this are invalid
            // Use 30 days TTL (max refresh token lifetime)
            redisTemplate.opsForValue().set(
                key,
                beforeTimestamp.epochSecond.toString(),
                30,
                TimeUnit.DAYS
            )
            log.info("Revoked all tokens for user before timestamp={}", beforeTimestamp)
        } catch (e: Exception) {
            log.error("Failed to revoke all user tokens in Redis: {}", e.message, e)
            throw e
        }
    }

    override fun isUserRevoked(email: String, issuedAt: Instant): Boolean {
        val key = userRevocationKey(email)
        return try {
            val revocationTimestamp = redisTemplate.opsForValue().get(key)?.toLongOrNull()
            if (revocationTimestamp != null) {
                // Token is revoked if it was issued before the revocation timestamp
                issuedAt.epochSecond < revocationTimestamp
            } else {
                false
            }
        } catch (e: Exception) {
            log.warn("Redis user revocation check failed, allowing request: {}", e.message)
            // Fail open: if Redis is down, don't block all authentication
            false
        }
    }

    /**
     * Generate Redis key for a specific token.
     * Tokens are hashed with SHA-256 to prevent token leakage from Redis dumps.
     */
    private fun tokenKey(token: String): String {
        val hash = hashToken(token)
        return "auth:revoked:token:$hash"
    }

    /**
     * Generate Redis key for user-level revocation.
     */
    private fun userRevocationKey(email: String): String {
        // Hash email for additional privacy
        val emailHash = hashToken(email.lowercase().trim())
        return "auth:revoked:user:$emailHash"
    }

    /**
     * Hash a string using SHA-256.
     */
    private fun hashToken(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val TOKEN_KEY_PREFIX = "auth:revoked:token:"
        private const val USER_KEY_PREFIX = "auth:revoked:user:"
    }
}
