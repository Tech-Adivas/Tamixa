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
@Table(name = "trial_usage")
class TrialUsageEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,

    @Column(name = "device_hash", nullable = false, length = 64)
    val deviceHash: String,

    @Column(name = "email_hash", nullable = false, length = 64)
    val emailHash: String,

    @Column(name = "used_at", nullable = false)
    val usedAt: Instant = Instant.now()
)
