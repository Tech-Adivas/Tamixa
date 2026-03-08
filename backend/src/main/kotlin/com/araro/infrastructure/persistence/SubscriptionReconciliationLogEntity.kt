package com.araro.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "subscription_reconciliation_log")
class SubscriptionReconciliationLogEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    val subscription: SubscriptionEntity,

    @Column(name = "provider_status", nullable = false, length = 32)
    val providerStatus: String,

    @Column(name = "db_status", nullable = false, length = 32)
    val dbStatus: String,

    @Column(name = "mismatch_flag", nullable = false)
    val mismatchFlag: Boolean = false,

    @Column(name = "checked_at", nullable = false)
    val checkedAt: Instant = Instant.now()
)
