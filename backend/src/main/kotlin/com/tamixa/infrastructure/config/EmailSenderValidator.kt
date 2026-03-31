package com.tamixa.infrastructure.config

import com.tamixa.infrastructure.notification.LoggingEmailSender
import com.tamixa.application.port.EmailSenderPort
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Validates that a real email sender is configured in production.
 * Fails startup with a clear message when passwordless auth is enabled
 * but only the dev LoggingEmailSender is wired (magic links would silently not be sent).
 */
@Component
@Profile("prod")
class EmailSenderValidator(
    private val emailSender: EmailSenderPort,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun validate() {
        val webBaseUrl = appProperties.auth.webBaseUrl
        val isProductionUrl = webBaseUrl.contains("tamixa.com") ||
            (!webBaseUrl.contains("localhost") && !webBaseUrl.contains("127.0.0.1"))

        if (isProductionUrl && emailSender is LoggingEmailSender) {
            val msg = "STARTUP VALIDATION FAILED: Production URL '$webBaseUrl' is configured for " +
                "passwordless auth but no real email sender is wired. " +
                "Set SENDGRID_API_KEY (or another email provider) so magic links are actually delivered. " +
                "Without this, passwordless login silently fails for all users."
            log.error(msg)
            throw IllegalStateException(msg)
        }

        log.info("Email sender validation passed: provider={}", emailSender::class.simpleName)
    }
}
