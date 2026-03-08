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
@Table(name = "data_export_job")
class DataExportJobEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    val parent: ParentEntity,
    @Column(nullable = false, length = 20)
    var status: String = "pending",
    @Column(name = "requested_at", nullable = false)
    val requestedAt: Instant = Instant.now(),
    @Column(name = "completed_at")
    var completedAt: Instant? = null,
    @Column(name = "download_url", columnDefinition = "TEXT")
    var downloadUrl: String? = null,
    @Column(name = "storage_key", columnDefinition = "TEXT")
    var storageKey: String? = null,
    @Column(name = "expires_at")
    var expiresAt: Instant? = null
)
