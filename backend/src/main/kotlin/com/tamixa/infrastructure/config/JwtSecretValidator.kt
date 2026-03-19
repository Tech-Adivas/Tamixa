package com.tamixa.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * In production, JWT secret must be set via JWT_SECRET environment variable.
 * Fails startup if prod profile is active and the default dev secret is still in use.
 */
@Component
@Profile("prod")
class JwtSecretValidator(
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val DEV_DEFAULT_SECRET =
            "tamixa-secret-key-min-256-bits-for-hs256-algorithm-please-change-in-production"
    }

    @EventListener(ApplicationReadyEvent::class)
    fun validateSecret() {
        val jwtSecret = appProperties.jwt.secret
        if (jwtSecret == DEV_DEFAULT_SECRET) {
            log.error("Production profile is active but JWT_SECRET is the default dev value. Set JWT_SECRET environment variable to a strong secret.")
            throw IllegalStateException(
                "JWT_SECRET must be set in production. Do not use the default dev secret."
            )
        }
    }
}
