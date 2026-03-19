package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.WebhookEventPort
import com.tamixa.application.port.WebhookEventStatus
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.webhook.WebhookPayloadEncryptor
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
