package com.tamixa.infrastructure.persistence

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
@Table(name = "invoices")
class InvoiceEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "provider_invoice_id", nullable = false, unique = true, length = 255)
    val providerInvoiceId: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    val subscription: SubscriptionEntity? = null,

    @Column(name = "amount_minor", nullable = false)
    val amountMinor: Long,

    @Column(nullable = false, length = 8)
    val currency: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: com.tamixa.domain.InvoiceStatus,

    @Column(name = "paid_at")
    val paidAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
