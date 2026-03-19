package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "admin_audit")
class AdminAuditEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "admin_email", nullable = false, length = 255)
    val adminEmail: String,

    @Column(nullable = false, length = 100)
    val action: String,

    @Column(name = "resource_type", nullable = false, length = 50)
    val resourceType: String,

    @Column(name = "resource_id", length = 100)
    val resourceId: String? = null,

    @Column(columnDefinition = "TEXT")
    val details: String? = null,

    @Column(name = "trace_id", length = 100)
    val traceId: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
