package com.tamixa.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * In production, CORS must not be wide open: require explicit [AppProperties.cors] configuration.
 * Fails if `allowed-origins` is unset or "*" and `allowed-origin-patterns` is also empty.
 */
@Component
@Profile("prod")
class CorsOriginValidator(
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun validateOrigins() {
        val origins = appProperties.cors.allowedOrigins.trim()
        val patternList = appProperties.cors.allowedOriginPatterns
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val originsOpen = origins.isEmpty() || origins == "*"
        if (originsOpen && patternList.isEmpty()) {
            log.error(
                "Production profile is active but CORS is not restricted: CORS_ALLOWED_ORIGINS is unset or '*' " +
                    "and CORS_ALLOWED_ORIGIN_PATTERNS is empty. Set explicit origins and/or patterns."
            )
            throw IllegalStateException(
                "Production CORS: set CORS_ALLOWED_ORIGINS (comma-separated HTTPS origins, not *) " +
                    "and/or CORS_ALLOWED_ORIGIN_PATTERNS."
            )
        }
    }
}
