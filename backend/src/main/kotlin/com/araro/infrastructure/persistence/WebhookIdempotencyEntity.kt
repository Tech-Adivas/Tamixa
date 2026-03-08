package com.araro.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "webhook_idempotency")
class WebhookIdempotencyEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 16)
    val provider: String,

    @Column(name = "event_id", nullable = false, length = 255)
    val eventId: String,

    @Column(name = "processed_at", nullable = false)
    val processedAt: Instant = Instant.now()
)
