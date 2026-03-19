package com.tamixa.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * When Stripe webhooks are enabled and (1) require-webhook-payload-encryption is true, or
 * (2) profile is prod, fails startup if SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY is not set.
 * PCI/compliance: webhook payloads stored at rest should be encrypted when handling payment events.
 */
@Component
@Order(0)
class WebhookEncryptionValidator(
    private val appProperties: AppProperties,
    private val environment: Environment
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun validateWebhookEncryption() {
        val sub = appProperties.subscription
        if (!sub.stripe.enabled || sub.stripe.webhookSecret.isBlank()) return
        if (sub.webhookPayloadEncryptionKey.isNotBlank()) return
        val required = sub.requireWebhookPayloadEncryption ||
            environment.activeProfiles.contains("prod")
        if (!required) return
        log.error(
            "Stripe webhooks are enabled (prod or require-webhook-payload-encryption=true) " +
                "but SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY is not set. " +
                "Set a 32-byte Base64 AES key to encrypt webhook payloads at rest (PCI/compliance)."
        )
        throw IllegalStateException(
            "SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY must be set when Stripe webhooks are enabled in production. " +
                "See app.subscription.require-webhook-payload-encryption and SUBSCRIPTION_WEBHOOK_ENCRYPTION_KEY."
        )
    }
}
