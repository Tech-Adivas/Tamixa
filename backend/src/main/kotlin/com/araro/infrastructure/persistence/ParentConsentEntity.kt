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
@Table(name = "parent_consent")
class ParentConsentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,
    @Column(name = "consent_type", nullable = false, length = 50)
    val consentType: String,
    @Column(nullable = false)
    val version: Int = 1,
    @Column(name = "granted_at", nullable = false)
    val grantedAt: Instant = Instant.now(),
    @Column(name = "ip_address", length = 45)
    val ipAddress: String? = null
)
