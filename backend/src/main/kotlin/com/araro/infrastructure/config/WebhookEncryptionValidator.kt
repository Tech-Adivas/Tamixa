package com.araro.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * When Stripe webhooks are enabled and webhook payload encryption is required (e.g. in production),
 * fails startup if SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY is not set.
 * PCI/compliance: webhook payloads stored at rest should be encrypted when handling payment events.
 */
@Component
@Order(0)
class WebhookEncryptionValidator(
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun validateWebhookEncryption() {
        val sub = appProperties.subscription
        if (!sub.requireWebhookPayloadEncryption) return
        if (!sub.stripe.enabled || sub.stripe.webhookSecret.isBlank()) return
        if (sub.webhookPayloadEncryptionKey.isNotBlank()) return
        log.error(
            "Stripe webhooks are enabled and require-webhook-payload-encryption is true, " +
                "but SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY is not set. " +
                "Set a 32-byte Base64 AES key to encrypt webhook payloads at rest (PCI/compliance)."
        )
        throw IllegalStateException(
            "SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY must be set when Stripe webhooks are enabled and " +
                "app.subscription.require-webhook-payload-encryption=true"
        )
    }
}
