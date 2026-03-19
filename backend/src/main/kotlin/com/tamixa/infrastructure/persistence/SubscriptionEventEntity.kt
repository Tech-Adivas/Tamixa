package com.tamixa.infrastructure.persistence

import com.tamixa.domain.PaymentProvider
import com.tamixa.domain.SubscriptionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "subscription_events")
class SubscriptionEventEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    val subscription: SubscriptionEntity,

    @Column(name = "event_type", nullable = false, length = 64)
    val eventType: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 32)
    val previousStatus: SubscriptionStatus? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 32)
    val newStatus: SubscriptionStatus? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val provider: PaymentProvider,

    @Column(name = "external_event_id", length = 255)
    val externalEventId: String? = null,

    @Column(columnDefinition = "TEXT")
    val reason: String? = null,

    @Column(columnDefinition = "TEXT")
    val payload: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
