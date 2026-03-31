package com.tamixa.application.analytics

import com.tamixa.api.admin.dto.RecordDoraDeploymentRequest
import com.tamixa.api.admin.dto.RecordDoraIncidentRequest
import com.tamixa.infrastructure.persistence.DoraDeploymentStatus
import com.tamixa.infrastructure.persistence.DoraEventType
import com.tamixa.infrastructure.persistence.DoraIncidentStatus
import com.tamixa.infrastructure.persistence.DoraMetricEventEntity
import com.tamixa.infrastructure.persistence.DoraMetricEventJpaRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class DoraMetricsServiceTest {

    private val repository: DoraMetricEventJpaRepository = mock()
    private val meterRegistry = SimpleMeterRegistry()
    private val service = DoraMetricsService(repository, meterRegistry)

    @Test
    fun `getMetrics computes deployment frequency cfr lead time and mttr`() {
        val now = Instant.parse("2026-03-31T10:00:00Z")
        val events = listOf(
            DoraMetricEventEntity(
                id = 1,
                eventType = DoraEventType.DEPLOYMENT,
                serviceName = "backend",
                environment = "production",
                deploymentStatus = DoraDeploymentStatus.SUCCESS,
                changeStartedAt = now.minusSeconds(2 * 3600),
                deployedAt = now.minusSeconds(3600),
                createdBy = "ci",
                createdAt = now.minusSeconds(3600)
            ),
            DoraMetricEventEntity(
                id = 2,
                eventType = DoraEventType.DEPLOYMENT,
                serviceName = "backend",
                environment = "production",
                deploymentStatus = DoraDeploymentStatus.SUCCESS,
                changeStartedAt = now.minusSeconds(90 * 60),
                deployedAt = now.minusSeconds(30 * 60),
                createdBy = "ci",
                createdAt = now.minusSeconds(30 * 60)
            ),
            DoraMetricEventEntity(
                id = 3,
                eventType = DoraEventType.DEPLOYMENT,
                serviceName = "backend",
                environment = "production",
                deploymentStatus = DoraDeploymentStatus.FAILED,
                changeStartedAt = now.minusSeconds(45 * 60),
                deployedAt = now.minusSeconds(20 * 60),
                createdBy = "ci",
                createdAt = now.minusSeconds(20 * 60)
            ),
            DoraMetricEventEntity(
                id = 4,
                eventType = DoraEventType.INCIDENT,
                serviceName = "backend",
                environment = "production",
                incidentStatus = DoraIncidentStatus.RESOLVED,
                incidentStartedAt = now.minusSeconds(50 * 60),
                restoredAt = now.minusSeconds(10 * 60),
                createdBy = "oncall",
                createdAt = now.minusSeconds(10 * 60)
            )
        )
        whenever(repository.findForWindow(any(), any(), any())).thenReturn(events)

        val result = service.getMetrics(days = 10, serviceName = "backend", environment = "production")

        assertThat(result.successfulDeployments).isEqualTo(2)
        assertThat(result.failedDeployments).isEqualTo(1)
        assertThat(result.deploymentFrequencyPerDay).isEqualTo(0.2) // 2/10
        assertThat(result.changeFailureRatePercent).isEqualTo(33.33) // 1/3
        assertThat(result.leadTimeMinutesP50).isEqualTo(60)
        assertThat(result.leadTimeMinutesP95).isEqualTo(60)
        assertThat(result.meanTimeToRestoreMinutes).isEqualTo(40)
        assertThat(result.resolvedIncidents).isEqualTo(1)
        assertThat(result.openIncidents).isEqualTo(0)
    }

    @Test
    fun `recordIncident requires restoredAt when status resolved`() {
        val request = RecordDoraIncidentRequest(
            serviceName = "backend",
            environment = "production",
            status = "RESOLVED",
            incidentStartedAt = Instant.parse("2026-03-31T08:00:00Z"),
            restoredAt = null,
            causedByChange = true
        )

        assertThatThrownBy {
            service.recordIncident("admin@tamixa.com", request)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("restoredAt is required")
    }

    @Test
    fun `recordDeployment saves normalized service and updates counters`() {
        val request = RecordDoraDeploymentRequest(
            serviceName = "BackEnd",
            environment = "Production",
            status = "SUCCESS",
            changeId = "sha-123",
            changeStartedAt = Instant.parse("2026-03-31T08:00:00Z"),
            deployedAt = Instant.parse("2026-03-31T09:30:00Z"),
            summary = "Release finished"
        )
        whenever(repository.save(any())).thenAnswer { it.arguments[0] as DoraMetricEventEntity }

        val response = service.recordDeployment("release-bot@tamixa.com", request)

        assertThat(response.eventType).isEqualTo("DEPLOYMENT")
        assertThat(response.serviceName).isEqualTo("backend")
        assertThat(response.environment).isEqualTo("production")
        assertThat(meterRegistry.find("dora_deployments_total").counter()?.count()).isEqualTo(1.0)
    }
}
