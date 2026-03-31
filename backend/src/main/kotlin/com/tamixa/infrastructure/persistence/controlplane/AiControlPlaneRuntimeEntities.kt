package com.tamixa.infrastructure.persistence.controlplane

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ai_workflow_runs")
class AiWorkflowRunEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    var project: AiProjectEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    var workflow: AiWorkflowEntity,

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = "",

    @Column(name = "entity_id", length = 120)
    var entityId: String? = null,

    @Column(name = "request_source", nullable = false, length = 40)
    var requestSource: String = "",

    @Column(nullable = false, length = 30)
    var status: String = "",

    @Column(name = "current_step_key", length = 120)
    var currentStepKey: String? = null,

    @Column(name = "requested_by", length = 120)
    var requestedBy: String? = null,

    @Column(name = "input_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var inputJson: Map<String, Any?> = emptyMap(),

    @Column(name = "output_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var outputJson: Map<String, Any?>? = null,

    @Column(name = "context_snapshot_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var contextSnapshotJson: Map<String, Any?>? = null,

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant = Instant.now(),

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Column(name = "failed_at")
    var failedAt: Instant? = null,

    @Column(name = "error_code", length = 80)
    var errorCode: String? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,
)

@Entity
@Table(name = "ai_workflow_step_runs")
class AiWorkflowStepRunEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_run_id", nullable = false)
    var workflowRun: AiWorkflowRunEntity,

    @Column(name = "step_key", nullable = false, length = 120)
    var stepKey: String = "",

    @Column(nullable = false, length = 30)
    var status: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    var provider: AiModelProviderEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    var model: AiModelEntity? = null,

    @Column(name = "prompt_asset_version_ids", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var promptAssetVersionIds: List<Any?>? = null,

    @Column(name = "input_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var inputJson: Map<String, Any?>? = null,

    @Column(name = "output_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var outputJson: Map<String, Any?>? = null,

    @Column(name = "validation_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var validationJson: Map<String, Any?>? = null,

    @Column(name = "evaluation_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var evaluationJson: Map<String, Any?>? = null,

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant = Instant.now(),

    @Column(name = "completed_at")
    var completedAt: Instant? = null,

    @Column(name = "error_code", length = 80)
    var errorCode: String? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,
)

@Entity
@Table(name = "ai_evaluation_results")
class AiEvaluationResultEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_run_id", nullable = false)
    var workflowStepRun: AiWorkflowStepRunEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    var profile: AiEvaluationProfileEntity,

    @Column(name = "overall_score", nullable = false, precision = 5, scale = 2)
    var overallScore: BigDecimal = BigDecimal.ZERO,

    @Column(name = "dimension_scores_json", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var dimensionScoresJson: Map<String, Any?> = emptyMap(),

    @Column(nullable = false, length = 30)
    var decision: String = "",

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_human_review_queue")
class AiHumanReviewQueueEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_run_id", nullable = false)
    var workflowRun: AiWorkflowRunEntity,

    @Column(name = "review_type", nullable = false, length = 50)
    var reviewType: String = "",

    @Column(nullable = false, length = 20)
    var priority: String = "",

    @Column(nullable = false, length = 30)
    var status: String = "",

    @Column(name = "reason_codes", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var reasonCodes: List<Any?>? = null,

    @Column(name = "assigned_to", length = 120)
    var assignedTo: String? = null,

    @Column(length = 30)
    var resolution: String? = null,

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    var resolutionNotes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null,
)

@Entity
@Table(name = "ai_cost_events")
class AiCostEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_step_run_id", nullable = false)
    var workflowStepRun: AiWorkflowStepRunEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id", nullable = false)
    var provider: AiModelProviderEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    var model: AiModelEntity? = null,

    @Column(name = "input_tokens")
    var inputTokens: Int? = 0,

    @Column(name = "output_tokens")
    var outputTokens: Int? = 0,

    @Column(name = "cached_input_tokens")
    var cachedInputTokens: Int? = 0,

    @Column(name = "latency_ms")
    var latencyMs: Int? = null,

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 6)
    var estimatedCost: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, length = 10)
    var currency: String = "USD",

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)

@Entity
@Table(name = "ai_audit_events")
class AiAuditEventEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", nullable = false, updatable = false)
    var id: UUID? = null,

    @Column(name = "event_type", nullable = false, length = 60)
    var eventType: String = "",

    @Column(name = "actor_type", nullable = false, length = 30)
    var actorType: String = "",

    @Column(name = "actor_id", length = 120)
    var actorId: String? = null,

    @Column(name = "entity_type", nullable = false, length = 60)
    var entityType: String = "",

    @Column(name = "entity_id", nullable = false, length = 120)
    var entityId: String = "",

    @Column(nullable = false, length = 60)
    var action: String = "",

    @Column(name = "details_json", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    var detailsJson: Map<String, Any?>? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
)
