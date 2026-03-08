package com.araro.infrastructure.persistence

import com.araro.application.port.WebhookEventPort
import com.araro.application.port.WebhookEventStatus
import com.araro.infrastructure.config.AppProperties
import com.araro.infrastructure.webhook.WebhookPayloadEncryptor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class WebhookEventRepositoryAdapter(
    private val webhookEventJpaRepository: WebhookEventJpaRepository,
    private val appProperties: AppProperties
) : WebhookEventPort {

    @Transactional
    override fun record(
        provider: String,
        eventId: String,
        eventType: String,
        status: WebhookEventStatus,
        rawPayload: String?
    ) {
        val encrypted = rawPayload?.let { WebhookPayloadEncryptor.encryptIfConfigured(appProperties.subscription.webhookPayloadEncryptionKey, it) }
        val entity = WebhookEventEntity(
            provider = provider,
            eventId = eventId,
            eventType = eventType,
            status = status.name,
            rawPayloadEncrypted = encrypted
        )
        webhookEventJpaRepository.save(entity)
    }
}
