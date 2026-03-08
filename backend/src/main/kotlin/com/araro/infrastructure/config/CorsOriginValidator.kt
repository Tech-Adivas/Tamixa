package com.araro.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * In production, CORS must be restricted to explicit origins.
 * Fails startup if prod profile is active and CORS_ALLOWED_ORIGINS is unset or "*".
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
        if (origins.isEmpty() || origins == "*") {
            log.error(
                "Production profile is active but CORS_ALLOWED_ORIGINS is not set or is '*'. " +
                    "Set CORS_ALLOWED_ORIGINS to comma-separated origins, e.g. https://app.araro.com,https://admin.araro.com"
            )
            throw IllegalStateException(
                "CORS_ALLOWED_ORIGINS must be set in production with explicit origins. Do not use '*'."
            )
        }
    }
}
