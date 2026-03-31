package com.tamixa.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * In production, dev OTP and passwordless bypass codes must not be set.
 * Fails startup if `DEV_OTP_CODE` or `DEV_PASSWORDLESS_CODE` is non-blank when profile `prod` is active.
 */
@Component
@Profile("prod")
class ProductionDevBypassValidator(
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun validateNoDevBypassInProd() {
        val otp = appProperties.auth.devOtpCode?.trim().orEmpty()
        val passwordless = appProperties.auth.devPasswordlessCode?.trim().orEmpty()
        if (otp.isNotEmpty()) {
            log.error("Production profile is active but DEV_OTP_CODE / app.auth.dev-otp-code is set. Remove it in production.")
            throw IllegalStateException(
                "DEV_OTP_CODE must not be set in production. It allows any phone to log in with a fixed code."
            )
        }
        if (passwordless.isNotEmpty()) {
            log.error(
                "Production profile is active but DEV_PASSWORDLESS_CODE / app.auth.dev-passwordless-code is set. Remove it in production."
            )
            throw IllegalStateException(
                "DEV_PASSWORDLESS_CODE must not be set in production. It allows fixed-code web login for any email."
            )
        }
    }
}
