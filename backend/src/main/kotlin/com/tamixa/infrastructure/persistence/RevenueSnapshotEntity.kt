package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "revenue_snapshots")
class RevenueSnapshotEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "snapshot_date", nullable = false, unique = true)
    var snapshotDate: LocalDate = LocalDate.now(),

    @Column(name = "mrr_minor", nullable = false)
    var mrrMinor: Long = 0,

    @Column(name = "active_subscriptions", nullable = false)
    var activeSubscriptions: Int = 0,

    @Column(name = "churn_rate", nullable = false, precision = 5, scale = 4)
    var churnRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "trial_conversion_rate", nullable = false, precision = 5, scale = 4)
    var trialConversionRate: BigDecimal = BigDecimal.ZERO,

    @Column(name = "arpu_minor", nullable = false)
    var arpuMinor: Long = 0,

    @Column(name = "stories_free", nullable = false)
    var storiesFree: Int = 0,

    @Column(name = "stories_premium", nullable = false)
    var storiesPremium: Int = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)
