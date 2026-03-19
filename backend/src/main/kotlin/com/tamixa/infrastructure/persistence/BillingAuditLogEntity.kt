package com.tamixa.infrastructure.persistence

import com.tamixa.domain.BillingEventType
import com.tamixa.domain.PaymentProvider
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
@Table(name = "billing_audit_log")
class BillingAuditLogEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    val subscription: SubscriptionEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 48)
    val eventType: BillingEventType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val provider: PaymentProvider,

    @Column(name = "external_id", length = 255)
    val externalId: String? = null,

    @Column(name = "amount_minor")
    val amountMinor: Long? = null,

    @Column(length = 8)
    val currency: String? = null,

    @Column(name = "invoice_id", length = 255)
    val invoiceId: String? = null,

    @Column(columnDefinition = "TEXT")
    val details: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
