package com.araro.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "webhook_events")
class WebhookEventEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 16)
    val provider: String,

    @Column(name = "event_id", nullable = false, length = 255)
    val eventId: String,

    @Column(name = "event_type", nullable = false, length = 128)
    val eventType: String,

    @Column(name = "processed_at", nullable = false)
    val processedAt: Instant = Instant.now(),

    @Column(nullable = false, length = 32)
    val status: String,

    @Column(name = "raw_payload_encrypted", columnDefinition = "TEXT")
    val rawPayloadEncrypted: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
