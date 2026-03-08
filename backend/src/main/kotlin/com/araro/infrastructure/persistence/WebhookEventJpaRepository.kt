package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface WebhookEventJpaRepository : JpaRepository<WebhookEventEntity, Long>
