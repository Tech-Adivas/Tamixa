package com.tamixa.api.controller

import com.tamixa.api.ApiVersion
import com.tamixa.api.admin.dto.AiProjectSummaryDto
import com.tamixa.api.admin.dto.AiWorkflowRunSummaryDto
import com.tamixa.api.admin.dto.AiWorkflowSummaryDto
import com.tamixa.api.admin.dto.ExecuteWorkflowRequest
import com.tamixa.api.admin.dto.PagedResponse
import com.tamixa.api.admin.dto.PromptVersionDto
import com.tamixa.api.admin.dto.PublishPromptRequest
import com.tamixa.api.admin.dto.WorkflowRunDetailDto
import com.tamixa.api.admin.dto.WorkflowRunStartedDto
import com.tamixa.application.controlplane.AiControlPlaneEntityNotFoundException
import com.tamixa.application.controlplane.AiControlPlaneQueryService
import com.tamixa.controlplane.application.port.ControlPlanePromptRegistryService
import com.tamixa.controlplane.application.port.ControlPlaneWorkflowService
import com.tamixa.controlplane.application.port.PublishPromptVersionCommand
import com.tamixa.controlplane.application.port.WorkflowExecutionRequest
import com.tamixa.infrastructure.persistence.controlplane.AiProjectJpaRepository
import com.tamixa.infrastructure.persistence.controlplane.AiWorkflowJpaRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("${ApiVersion.V1}/admin/ai-control-plane")
@Validated
class AiControlPlaneAdminController(
    private val workflowService: ControlPlaneWorkflowService,
    private val promptRegistry: ControlPlanePromptRegistryService,
    private val queryService: AiControlPlaneQueryService,
    private val projectRepository: AiProjectJpaRepository,
    private val workflowRepository: AiWorkflowJpaRepository,
) {

    @GetMapping("/projects")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_AI_CONTROL_PLANE')")
    fun listProjects(): List<AiProjectSummaryDto> =
        projectRepository.findAll().map { p ->
            AiProjectSummaryDto(
                id = p.id!!,
                code = p.code,
                name = p.name,
                status = p.status,
            )
        }

    @GetMapping("/projects/{code}/workflows")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_AI_CONTROL_PLANE')")
    fun listWorkflows(@PathVariable code: String): List<AiWorkflowSummaryDto> {
        val project = projectRepository.findByCode(code.trim())
            ?: throw AiControlPlaneEntityNotFoundException("project not found: $code")
        return workflowRepository.findAllByProject_Id(project.id!!).map { w ->
            AiWorkflowSummaryDto(
                id = w.id!!,
                workflowKey = w.workflowKey,
                name = w.name,
                category = w.category,
                status = w.status,
                version = w.version,
            )
        }
    }

    @GetMapping("/workflow-runs")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_AI_CONTROL_PLANE')")
    fun listWorkflowRuns(
        @RequestParam projectCode: String,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): PagedResponse<AiWorkflowRunSummaryDto> {
        val paged = queryService.listWorkflowRuns(projectCode, page, size)
        return PagedResponse(
            content = paged.content.map { v ->
                AiWorkflowRunSummaryDto(
                    id = v.id,
                    workflowKey = v.workflowKey,
                    status = v.status,
                    currentStepKey = v.currentStepKey,
                    entityType = v.entityType,
                    entityId = v.entityId,
                    startedAtIso = v.startedAtIso,
                )
            },
            page = paged.page,
            size = paged.size,
            totalElements = paged.totalElements,
            totalPages = paged.totalPages,
            first = paged.first,
            last = paged.last,
        )
    }

    @PostMapping("/workflow-runs/execute")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_AI_CONTROL_PLANE')")
    fun executeWorkflow(@Valid @RequestBody body: ExecuteWorkflowRequest): WorkflowRunStartedDto {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        val result = workflowService.execute(
            WorkflowExecutionRequest(
                projectCode = body.projectCode,
                workflowKey = body.workflowKey,
                entityType = body.entityType,
                entityId = body.entityId,
                requestedBy = actor,
                input = body.input,
            )
        )
        return WorkflowRunStartedDto(
            workflowRunId = result.workflowRunId,
            status = result.status,
            currentStep = result.currentStep,
            trackingPath = result.trackingPath,
        )
    }

    @GetMapping("/workflow-runs/{runId}")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_AI_CONTROL_PLANE')")
    fun getWorkflowRun(@PathVariable runId: UUID): WorkflowRunDetailDto {
        val v = workflowService.getRun(runId)
        return WorkflowRunDetailDto(
            workflowRunId = v.workflowRunId,
            status = v.status,
            currentStepKey = v.currentStepKey,
            output = v.output,
            startedAtIso = v.startedAtIso,
            completedAtIso = v.completedAtIso,
        )
    }

    @PostMapping("/workflow-runs/{runId}/cancel")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_AI_CONTROL_PLANE')")
    fun cancelWorkflowRun(@PathVariable runId: UUID): ResponseEntity<Void> {
        workflowService.cancel(runId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/workflow-runs/{runId}/retry")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_AI_CONTROL_PLANE')")
    fun retryWorkflowRun(@PathVariable runId: UUID): WorkflowRunStartedDto {
        val r = workflowService.retry(runId)
        return WorkflowRunStartedDto(
            workflowRunId = r.workflowRunId,
            status = r.status,
            currentStep = r.currentStep,
            trackingPath = r.trackingPath,
        )
    }

    @GetMapping("/prompts/resolve")
    @PreAuthorize("@adminAuth.hasPermission('VIEW_AI_CONTROL_PLANE')")
    fun resolvePrompt(
        @RequestParam projectCode: String,
        @RequestParam assetKey: String,
        @RequestParam(required = false) version: Int?,
    ): PromptVersionDto {
        val v = promptRegistry.resolve(projectCode, assetKey, version)
        return PromptVersionDto(
            assetId = v.assetId,
            assetKey = v.assetKey,
            version = v.version,
            checksum = v.checksum,
            approvalStatus = v.approvalStatus,
        )
    }

    @PostMapping("/prompts/publish")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_AI_CONTROL_PLANE')")
    fun publishPrompt(@Valid @RequestBody body: PublishPromptRequest): PromptVersionDto {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        val v = promptRegistry.publishVersion(
            PublishPromptVersionCommand(
                projectCode = body.projectCode,
                assetKey = body.assetKey,
                content = body.content,
                metadata = body.metadata,
                createdBy = actor,
            )
        )
        return PromptVersionDto(
            assetId = v.assetId,
            assetKey = v.assetKey,
            version = v.version,
            checksum = v.checksum,
            approvalStatus = v.approvalStatus,
        )
    }

    @PostMapping("/prompts/assets/{assetId}/versions/{version}/approve")
    @PreAuthorize("@adminAuth.hasPermission('MANAGE_AI_CONTROL_PLANE')")
    fun approvePromptVersion(
        @PathVariable assetId: UUID,
        @PathVariable version: Int,
    ): ResponseEntity<Void> {
        val actor = SecurityContextHolder.getContext().authentication?.name ?: "unknown"
        promptRegistry.approveVersion(assetId, version, actor)
        return ResponseEntity.noContent().build()
    }
}
