package com.tamixa.api.admin.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class AiProjectSummaryDto(
    val id: UUID,
    val code: String,
    val name: String,
    val status: String,
)

data class AiWorkflowSummaryDto(
    val id: UUID,
    val workflowKey: String,
    val name: String,
    val category: String,
    val status: String,
    val version: Int,
)

data class AiWorkflowRunSummaryDto(
    val id: UUID,
    val workflowKey: String,
    val status: String,
    val currentStepKey: String?,
    val entityType: String,
    val entityId: String?,
    val startedAtIso: String,
)

data class ExecuteWorkflowRequest(
    @field:NotBlank val projectCode: String,
    @field:NotBlank val workflowKey: String,
    @field:NotBlank val entityType: String,
    val entityId: String? = null,
    val input: Map<String, Any?> = emptyMap(),
)

data class WorkflowRunStartedDto(
    val workflowRunId: UUID,
    val status: String,
    val currentStep: String?,
    val trackingPath: String,
)

data class WorkflowRunDetailDto(
    val workflowRunId: UUID,
    val status: String,
    val currentStepKey: String?,
    val output: Map<String, Any?>?,
    val startedAtIso: String?,
    val completedAtIso: String?,
)

data class PromptVersionDto(
    val assetId: UUID,
    val assetKey: String,
    val version: Int,
    val checksum: String,
    val approvalStatus: String,
)

data class PublishPromptRequest(
    @field:NotBlank val projectCode: String,
    @field:NotBlank val assetKey: String,
    @field:NotBlank val content: String,
    val metadata: Map<String, Any?>? = null,
)
