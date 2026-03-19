package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "processing_job")
class ProcessingJobEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "job_type", nullable = false, length = 50)
    val jobType: String,

    @Column(name = "resource_type", nullable = false, length = 32)
    val resourceType: String,

    @Column(name = "resource_id", nullable = false, length = 128)
    val resourceId: String,

    @Column(nullable = false, length = 30)
    var status: String = "PENDING",

    @Column(nullable = false)
    var progress: Int = 0,

    @Column(name = "started_at")
    var startedAt: Instant? = null,

    @Column(name = "finished_at")
    var finishedAt: Instant? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(columnDefinition = "TEXT")
    var metadata: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
