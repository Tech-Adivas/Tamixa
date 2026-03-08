package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface WebhookIdempotencyJpaRepository : JpaRepository<WebhookIdempotencyEntity, Long> {

    fun existsByProviderAndEventId(provider: String, eventId: String): Boolean
}
