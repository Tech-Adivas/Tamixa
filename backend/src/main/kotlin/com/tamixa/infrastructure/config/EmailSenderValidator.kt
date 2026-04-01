package com.tamixa.infrastructure.config

import com.tamixa.infrastructure.notification.SendGridEmailSender
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
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
    private val sendGridEmailSender: ObjectProvider<SendGridEmailSender>,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun validate() {
        val webBaseUrl = appProperties.auth.webBaseUrl
        val isProductionUrl = webBaseUrl.contains("tamixa.com") ||
            (!webBaseUrl.contains("localhost") && !webBaseUrl.contains("127.0.0.1"))

        val remote = appProperties.notificationsEmail
        val remoteOk = remote.remoteEnabled &&
            remote.baseUrl.isNotBlank() &&
            remote.internalApiKey.isNotBlank()

        if (isProductionUrl && sendGridEmailSender.getIfAvailable() == null && !remoteOk) {
            val msg = "STARTUP VALIDATION FAILED: Production URL '$webBaseUrl' is configured for " +
                "passwordless auth but no real email sender is wired. " +
                "Set SENDGRID_API_KEY on the API (or enable app.notifications-email.remote-* toward the notifications service). " +
                "Without this, passwordless login silently fails for all users."
            log.error(msg)
            throw IllegalStateException(msg)
        }

        val providerName = when {
            remoteOk -> "RemoteNotificationsEmailSender (notifications service)"
            sendGridEmailSender.getIfAvailable() != null -> sendGridEmailSender.getIfAvailable()!!::class.simpleName
            else -> "LoggingEmailSender (async facade)"
        }
        log.info("Email sender validation passed: provider={}", providerName)
    }
}
