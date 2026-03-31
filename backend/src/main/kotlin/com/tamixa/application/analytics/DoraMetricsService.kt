package com.tamixa.application.analytics

import com.tamixa.api.admin.dto.DoraMetricEventResponseDto
import com.tamixa.api.admin.dto.DoraMetricsDto
import com.tamixa.api.admin.dto.RecordDoraDeploymentRequest
import com.tamixa.api.admin.dto.RecordDoraIncidentRequest
import com.tamixa.infrastructure.persistence.DoraDeploymentStatus
import com.tamixa.infrastructure.persistence.DoraEventType
import com.tamixa.infrastructure.persistence.DoraIncidentStatus
import com.tamixa.infrastructure.persistence.DoraMetricEventEntity
import com.tamixa.infrastructure.persistence.DoraMetricEventJpaRepository
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import kotlin.math.round

@Service
class DoraMetricsService(
    private val doraMetricEventJpaRepository: DoraMetricEventJpaRepository,
    private val meterRegistry: MeterRegistry
) {
    @Transactional(readOnly = true)
    fun getMetrics(days: Int, serviceName: String?, environment: String?): DoraMetricsDto {
        val safeDays = days.coerceIn(1, 365)
        val normalizedService = normalizeFilter(serviceName)
        val normalizedEnvironment = normalizeFilter(environment)
        val windowStart = Instant.now().minus(Duration.ofDays(safeDays.toLong()))

        val events = doraMetricEventJpaRepository.findForWindow(windowStart, normalizedService, normalizedEnvironment)
        val deploymentEvents = events.filter { it.eventType == DoraEventType.DEPLOYMENT }
        val incidentEvents = events.filter { it.eventType == DoraEventType.INCIDENT }

        val successfulDeployments = deploymentEvents.count { it.deploymentStatus == DoraDeploymentStatus.SUCCESS }.toLong()
        val failedDeployments = deploymentEvents.count { it.deploymentStatus == DoraDeploymentStatus.FAILED }.toLong()
        val totalDeployments = successfulDeployments + failedDeployments

        val leadTimesMinutes = deploymentEvents.mapNotNull { deployment ->
            if (deployment.deploymentStatus != DoraDeploymentStatus.SUCCESS) return@mapNotNull null
            val startedAt = deployment.changeStartedAt ?: return@mapNotNull null
            val deployedAt = deployment.deployedAt ?: return@mapNotNull null
            if (startedAt.isAfter(deployedAt)) return@mapNotNull null
            Duration.between(startedAt, deployedAt).toMinutes()
        }

        val resolvedIncidents = incidentEvents.count { it.incidentStatus == DoraIncidentStatus.RESOLVED }.toLong()
        val openIncidents = incidentEvents.count { it.incidentStatus == DoraIncidentStatus.OPEN }.toLong()
        val mttrMinutes = incidentEvents.mapNotNull { incident ->
            if (incident.incidentStatus != DoraIncidentStatus.RESOLVED) return@mapNotNull null
            val startedAt = incident.incidentStartedAt ?: return@mapNotNull null
            val restoredAt = incident.restoredAt ?: return@mapNotNull null
            if (startedAt.isAfter(restoredAt)) return@mapNotNull null
            Duration.between(startedAt, restoredAt).toMinutes()
        }

        val deploymentFrequencyPerDay = roundToTwo(successfulDeployments.toDouble() / safeDays.toDouble())
        val changeFailureRate = if (totalDeployments == 0L) 0.0 else roundToTwo((failedDeployments * 100.0) / totalDeployments)
        val meanTimeToRestoreMinutes = mttrMinutes.takeIf { it.isNotEmpty() }?.average()?.let { round(it).toLong() }

        return DoraMetricsDto(
            windowDays = safeDays,
            serviceName = normalizedService,
            environment = normalizedEnvironment,
            successfulDeployments = successfulDeployments,
            failedDeployments = failedDeployments,
            deploymentFrequencyPerDay = deploymentFrequencyPerDay,
            changeFailureRatePercent = changeFailureRate,
            leadTimeMinutesP50 = percentile(leadTimesMinutes, 50.0),
            leadTimeMinutesP95 = percentile(leadTimesMinutes, 95.0),
            meanTimeToRestoreMinutes = meanTimeToRestoreMinutes,
            openIncidents = openIncidents,
            resolvedIncidents = resolvedIncidents
        )
    }

    @Transactional
    fun recordDeployment(adminEmail: String, request: RecordDoraDeploymentRequest): DoraMetricEventResponseDto {
        val normalizedService = normalizeRequired(request.serviceName, 80, "serviceName")
        val normalizedEnvironment = normalizeRequired(request.environment, 40, "environment")
        val status = parseDeploymentStatus(request.status)
        val deployedAt = request.deployedAt ?: Instant.now()
        val changeStartedAt = request.changeStartedAt ?: deployedAt
        if (changeStartedAt.isAfter(deployedAt)) {
            throw IllegalArgumentException("changeStartedAt must be before or equal to deployedAt")
        }

        val saved = doraMetricEventJpaRepository.save(
            DoraMetricEventEntity(
                eventType = DoraEventType.DEPLOYMENT,
                serviceName = normalizedService,
                environment = normalizedEnvironment,
                deploymentStatus = status,
                changeId = request.changeId?.trim()?.takeIf { it.isNotBlank() },
                changeStartedAt = changeStartedAt,
                deployedAt = deployedAt,
                summary = request.summary?.trim()?.takeIf { it.isNotBlank() },
                createdBy = adminEmail
            )
        )

        meterRegistry.counter(
            "dora_deployments_total",
            "service", normalizedService,
            "environment", normalizedEnvironment,
            "status", status.name.lowercase()
        ).increment()

        if (status == DoraDeploymentStatus.SUCCESS) {
            val leadTimeMs = Duration.between(changeStartedAt, deployedAt).toMillis()
            meterRegistry.timer(
                "dora_lead_time",
                "service", normalizedService,
                "environment", normalizedEnvironment
            ).record(leadTimeMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        }

        return DoraMetricEventResponseDto(
            id = saved.id,
            eventType = saved.eventType.name,
            serviceName = saved.serviceName,
            environment = saved.environment,
            createdAt = saved.createdAt
        )
    }

    @Transactional
    fun recordIncident(adminEmail: String, request: RecordDoraIncidentRequest): DoraMetricEventResponseDto {
        val normalizedService = normalizeRequired(request.serviceName, 80, "serviceName")
        val normalizedEnvironment = normalizeRequired(request.environment, 40, "environment")
        val status = parseIncidentStatus(request.status)
        if (status == DoraIncidentStatus.RESOLVED && request.restoredAt == null) {
            throw IllegalArgumentException("restoredAt is required when status is RESOLVED")
        }
        if (request.restoredAt != null && request.incidentStartedAt.isAfter(request.restoredAt)) {
            throw IllegalArgumentException("incidentStartedAt must be before or equal to restoredAt")
        }

        val saved = doraMetricEventJpaRepository.save(
            DoraMetricEventEntity(
                eventType = DoraEventType.INCIDENT,
                serviceName = normalizedService,
                environment = normalizedEnvironment,
                incidentStatus = status,
                incidentStartedAt = request.incidentStartedAt,
                restoredAt = request.restoredAt,
                causedByChange = request.causedByChange,
                summary = request.summary?.trim()?.takeIf { it.isNotBlank() },
                createdBy = adminEmail
            )
        )

        meterRegistry.counter(
            "dora_incidents_total",
            "service", normalizedService,
            "environment", normalizedEnvironment,
            "status", status.name.lowercase(),
            "causedByChange", request.causedByChange.toString()
        ).increment()

        if (status == DoraIncidentStatus.RESOLVED && request.restoredAt != null) {
            val mttrMs = Duration.between(request.incidentStartedAt, request.restoredAt).toMillis()
            meterRegistry.timer(
                "dora_mttr",
                "service", normalizedService,
                "environment", normalizedEnvironment
            ).record(mttrMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        }

        return DoraMetricEventResponseDto(
            id = saved.id,
            eventType = saved.eventType.name,
            serviceName = saved.serviceName,
            environment = saved.environment,
            createdAt = saved.createdAt
        )
    }

    private fun normalizeFilter(value: String?): String? =
        value?.trim()?.lowercase()?.takeIf { it.isNotBlank() }

    private fun normalizeRequired(value: String, maxLen: Int, field: String): String {
        val normalized = value.trim().lowercase()
        if (normalized.isBlank()) throw IllegalArgumentException("$field is required")
        return normalized.take(maxLen)
    }

    private fun parseDeploymentStatus(status: String): DoraDeploymentStatus =
        runCatching { DoraDeploymentStatus.valueOf(status.trim().uppercase()) }
            .getOrElse { throw IllegalArgumentException("status must be SUCCESS or FAILED") }

    private fun parseIncidentStatus(status: String): DoraIncidentStatus =
        runCatching { DoraIncidentStatus.valueOf(status.trim().uppercase()) }
            .getOrElse { throw IllegalArgumentException("status must be OPEN or RESOLVED") }

    private fun percentile(values: List<Long>, p: Double): Long? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val rank = (p / 100.0) * (sorted.size - 1)
        val idx = rank.toInt().coerceIn(0, sorted.lastIndex)
        return sorted[idx]
    }

    private fun roundToTwo(value: Double): Double = round(value * 100.0) / 100.0
}
