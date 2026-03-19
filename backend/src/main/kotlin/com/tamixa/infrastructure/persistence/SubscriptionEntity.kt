package com.tamixa.infrastructure.persistence

import com.tamixa.domain.PaymentProvider
import com.tamixa.domain.SubscriptionPlan
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
@Table(name = "subscriptions")
class SubscriptionEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false, unique = true)
    val parent: ParentEntity,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    val plan: SubscriptionPlan,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var status: SubscriptionStatus,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    val provider: PaymentProvider,

    @Column(name = "external_subscription_id", length = 255)
    val externalSubscriptionId: String? = null,

    @Column(name = "external_customer_id", length = 255)
    val externalCustomerId: String? = null,

    @Column(name = "current_period_start")
    val currentPeriodStart: Instant? = null,

    @Column(name = "current_period_end")
    var currentPeriodEnd: Instant? = null,

    @Column(name = "trial_end")
    val trialEnd: Instant? = null,

    @Column(name = "cancel_at_period_end", nullable = false)
    var cancelAtPeriodEnd: Boolean = false,

    @Column(name = "canceled_at")
    var canceledAt: Instant? = null,

    @Column(name = "past_due_at")
    var pastDueAt: Instant? = null,

    @Column(name = "max_children", nullable = false)
    val maxChildren: Int = 1,

    @Column(name = "voice_premium", nullable = false)
    val voicePremium: Boolean = false,

    @Column(name = "trial_used", nullable = false)
    var trialUsed: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
