package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.WebhookIdempotencyPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class WebhookIdempotencyAdapter(
    private val webhookIdempotencyJpaRepository: WebhookIdempotencyJpaRepository
) : WebhookIdempotencyPort {

    @Transactional
    override fun alreadyProcessed(provider: String, eventId: String): Boolean {
        return webhookIdempotencyJpaRepository.existsByProviderAndEventId(provider, eventId)
    }

    @Transactional
    override fun markProcessed(provider: String, eventId: String) {
        if (webhookIdempotencyJpaRepository.existsByProviderAndEventId(provider, eventId)) return
        webhookIdempotencyJpaRepository.save(
            WebhookIdempotencyEntity(provider = provider, eventId = eventId)
        )
    }
}
