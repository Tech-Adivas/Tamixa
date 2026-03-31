package com.tamixa.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "dora_metric_events")
class DoraMetricEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 24)
    val eventType: DoraEventType,

    @Column(name = "service_name", nullable = false, length = 80)
    val serviceName: String,

    @Column(name = "environment", nullable = false, length = 40)
    val environment: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "deployment_status", length = 20)
    val deploymentStatus: DoraDeploymentStatus? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "incident_status", length = 20)
    val incidentStatus: DoraIncidentStatus? = null,

    @Column(name = "change_id", length = 120)
    val changeId: String? = null,

    @Column(name = "change_started_at")
    val changeStartedAt: Instant? = null,

    @Column(name = "deployed_at")
    val deployedAt: Instant? = null,

    @Column(name = "incident_started_at")
    val incidentStartedAt: Instant? = null,

    @Column(name = "restored_at")
    val restoredAt: Instant? = null,

    @Column(name = "caused_by_change", nullable = false)
    val causedByChange: Boolean = true,

    @Column(name = "summary", length = 500)
    val summary: String? = null,

    @Column(name = "created_by", nullable = false, length = 255)
    val createdBy: String,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)

enum class DoraEventType {
    DEPLOYMENT,
    INCIDENT
}

enum class DoraDeploymentStatus {
    SUCCESS,
    FAILED
}

enum class DoraIncidentStatus {
    OPEN,
    RESOLVED
}
