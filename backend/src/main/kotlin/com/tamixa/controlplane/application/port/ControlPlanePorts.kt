package com.tamixa.controlplane.application.port

import java.util.UUID

/**
 * Hexagonal ports for the Tamixa AI Control Plane.
 *
 * Design: repository root `docs/CONTROL_PLANE_ARCHITECTURE.md`.
 * Integration status: `docs/AI_GOVERNANCE_E2E.md`.
 * Public prefix `/api/control-plane/` remains deny-all in `SecurityConfig`; admin API lives under `/api/v1/admin/ai-control-plane/`.
 */
data class WorkflowExecutionRequest(
    val projectCode: String,
    val workflowKey: String,
    val entityType: String,
    val entityId: String?,
    val requestedBy: String,
    val input: Map<String, Any?>,
    /** e.g. `ADMIN_API` (dashboard execute) or `STORY_SERVICE` (parent story generation spine). */
    val requestSource: String = "ADMIN_API",
)

data class WorkflowRunResult(
    val workflowRunId: UUID,
    val status: String,
    val currentStep: String?,
    val trackingPath: String,
)

data class WorkflowRunView(
    val workflowRunId: UUID,
    val status: String,
    val currentStepKey: String?,
    val output: Map<String, Any?>?,
    val startedAtIso: String?,
    val completedAtIso: String?,
)

interface ControlPlaneWorkflowService {
    fun execute(request: WorkflowExecutionRequest): WorkflowRunResult
    fun getRun(runId: UUID): WorkflowRunView
    fun retry(runId: UUID): WorkflowRunResult
    fun cancel(runId: UUID)
    /** Marks an in-progress run successful; joins the caller transaction when used from product code. */
    fun markRunCompleted(runId: UUID, outputJson: Map<String, Any?>)
    /**
     * Persists failure in a new transaction so the run is visible even when the caller rolls back
     * (e.g. story row reverted after a generation error).
     */
    fun markRunFailed(runId: UUID, errorCode: String?, errorMessage: String?)
}

data class PublishPromptVersionCommand(
    val projectCode: String,
    val assetKey: String,
    val content: String,
    val metadata: Map<String, Any?>?,
    val createdBy: String,
)

data class PromptAssetVersionView(
    val assetId: UUID,
    val assetKey: String,
    val version: Int,
    val checksum: String,
    val approvalStatus: String,
)

interface ControlPlanePromptRegistryService {
    fun resolve(projectCode: String, assetKey: String, version: Int? = null): PromptAssetVersionView
    fun publishVersion(command: PublishPromptVersionCommand): PromptAssetVersionView
    fun approveVersion(assetId: UUID, version: Int, approver: String)
}

data class PromptComposeRequest(
    val projectCode: String,
    val assetKeys: List<String>,
    val languageCode: String?,
    val variables: Map<String, String>,
)

data class ComposedPrompt(
    val system: String?,
    val user: String,
    val metadata: Map<String, Any?>,
)

interface ControlPlanePromptComposer {
    fun compose(request: PromptComposeRequest): ComposedPrompt
}

data class RoutingRequest(
    val taskType: String,
    val language: String?,
    val qualityTier: String?,
    val budgetTier: String?,
    val audienceType: String?,
)

data class RoutingDecision(
    val selectedProviderKey: String,
    val selectedModelKey: String,
    val fallbacks: List<Pair<String, String>>,
    val routingPolicyKey: String?,
)

interface ControlPlaneModelRoutingService {
    fun resolve(request: RoutingRequest): RoutingDecision
}

data class PolicyEvaluationRequest(
    val taskType: String,
    val context: Map<String, Any?>,
)

data class PolicyDecision(
    val allowed: Boolean,
    val decisionCode: String,
    val reasons: List<String>,
    val requiresHumanReview: Boolean,
)

interface ControlPlanePolicyEngine {
    fun evaluate(request: PolicyEvaluationRequest): PolicyDecision
}

data class EvaluationRequest(
    val taskType: String,
    val languageCode: String?,
    val profileKey: String,
    val outputText: String,
    val structuredOutput: Map<String, Any?>?,
)

data class EvaluationResultView(
    val overallScore: java.math.BigDecimal,
    val dimensionScores: Map<String, java.math.BigDecimal>,
    val decision: String,
    val notes: String?,
)

interface ControlPlaneEvaluationService {
    fun evaluate(request: EvaluationRequest): EvaluationResultView
}

data class CostEventCommand(
    val workflowStepRunId: UUID,
    val providerKey: String,
    val modelKey: String?,
    val inputTokens: Int,
    val outputTokens: Int,
    val cachedInputTokens: Int,
    val latencyMs: Int?,
    val estimatedCost: java.math.BigDecimal,
    val currency: String,
)

interface ControlPlaneCostTrackingService {
    fun record(event: CostEventCommand)
}

data class LlmGenerationRequest(
    val providerKey: String,
    val modelKey: String,
    val systemPrompt: String?,
    val userPrompt: String,
    val maxOutputTokens: Int?,
    val temperature: Double?,
)

data class LlmGenerationResponse(
    val text: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val cachedInputTokens: Int,
)

/**
 * Provider adapters must not be invoked from domain code directly; the orchestration layer uses these ports.
 */
interface ControlPlaneLlmProviderAdapter {
    fun generate(request: LlmGenerationRequest): LlmGenerationResponse
}
