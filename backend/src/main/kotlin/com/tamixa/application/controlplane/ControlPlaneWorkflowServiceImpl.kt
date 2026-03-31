package com.tamixa.application.controlplane

import com.tamixa.controlplane.application.port.ControlPlaneWorkflowService
import com.tamixa.controlplane.application.port.WorkflowExecutionRequest
import com.tamixa.controlplane.application.port.WorkflowRunResult
import com.tamixa.controlplane.application.port.WorkflowRunView
import com.tamixa.infrastructure.persistence.controlplane.AiAuditEventEntity
import com.tamixa.infrastructure.persistence.controlplane.AiProjectJpaRepository
import com.tamixa.infrastructure.persistence.controlplane.AiWorkflowJpaRepository
import com.tamixa.infrastructure.persistence.controlplane.AiWorkflowRunEntity
import com.tamixa.infrastructure.persistence.controlplane.AiWorkflowRunJpaRepository
import com.tamixa.infrastructure.persistence.controlplane.AiAuditEventJpaRepository
import com.tamixa.infrastructure.persistence.controlplane.AiWorkflowStepJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private const val STATUS_IN_PROGRESS = "IN_PROGRESS"
private const val STATUS_CANCELLED = "CANCELLED"
private const val STATUS_COMPLETED = "COMPLETED"
private const val STATUS_FAILED = "FAILED"

@Service
class ControlPlaneWorkflowServiceImpl(
    private val projectRepository: AiProjectJpaRepository,
    private val workflowRepository: AiWorkflowJpaRepository,
    private val workflowStepRepository: AiWorkflowStepJpaRepository,
    private val workflowRunRepository: AiWorkflowRunJpaRepository,
    private val auditEventRepository: AiAuditEventJpaRepository,
) : ControlPlaneWorkflowService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun execute(request: WorkflowExecutionRequest): WorkflowRunResult {
        val project = projectRepository.findByCode(request.projectCode.trim())
            ?: throw AiControlPlaneEntityNotFoundException("project not found: ${request.projectCode}")
        val workflow = workflowRepository.findByWorkflowKey(request.workflowKey.trim())
            ?: throw AiControlPlaneEntityNotFoundException("workflow not found: ${request.workflowKey}")
        if (workflow.project.id != project.id) {
            throw AiControlPlaneEntityNotFoundException("workflow ${request.workflowKey} does not belong to project ${request.projectCode}")
        }
        val steps = workflowStepRepository.findAllByWorkflow_IdOrderByStepOrderAsc(workflow.id!!)
        val firstStepKey = steps.firstOrNull()?.stepKey
        val source = request.requestSource.trim().ifBlank { "ADMIN_API" }
        val actorType = if (source == "STORY_SERVICE") "PARENT" else "ADMIN"
        val run = workflowRunRepository.save(
            AiWorkflowRunEntity(
                project = project,
                workflow = workflow,
                entityType = request.entityType,
                entityId = request.entityId,
                requestSource = source,
                status = STATUS_IN_PROGRESS,
                currentStepKey = firstStepKey,
                requestedBy = request.requestedBy,
                inputJson = request.input,
            )
        )
        val runId = run.id!!
        auditEventRepository.save(
            AiAuditEventEntity(
                eventType = "WORKFLOW_RUN_STARTED",
                actorType = actorType,
                actorId = request.requestedBy,
                entityType = "AI_WORKFLOW_RUN",
                entityId = runId.toString(),
                action = "EXECUTE",
                detailsJson = mapOf(
                    "projectCode" to request.projectCode,
                    "workflowKey" to request.workflowKey,
                    "entityType" to request.entityType,
                ),
            )
        )
        log.info(
            "control_plane_workflow_run_started runId={} workflowKey={} projectCode={}",
            runId,
            request.workflowKey,
            request.projectCode,
        )
        val trackingPath = "/api/v1/admin/ai-control-plane/workflow-runs/$runId"
        return WorkflowRunResult(
            workflowRunId = runId,
            status = STATUS_IN_PROGRESS,
            currentStep = firstStepKey,
            trackingPath = trackingPath,
        )
    }

    @Transactional(readOnly = true)
    override fun getRun(runId: UUID): WorkflowRunView {
        val run = workflowRunRepository.findById(runId).orElseThrow {
            AiControlPlaneEntityNotFoundException("workflow run not found: $runId")
        }
        return WorkflowRunView(
            workflowRunId = run.id!!,
            status = run.status,
            currentStepKey = run.currentStepKey,
            output = run.outputJson,
            startedAtIso = run.startedAt.toString(),
            completedAtIso = run.completedAt?.toString(),
        )
    }

    override fun retry(runId: UUID): WorkflowRunResult {
        throw UnsupportedOperationException("workflow retry is not implemented yet; create a new run instead")
    }

    @Transactional
    override fun cancel(runId: UUID) {
        val run = workflowRunRepository.findById(runId).orElseThrow {
            AiControlPlaneEntityNotFoundException("workflow run not found: $runId")
        }
        if (run.status != STATUS_IN_PROGRESS) {
            throw AiControlPlaneConflictException("cannot cancel run in status ${run.status}")
        }
        run.status = STATUS_CANCELLED
        run.completedAt = java.time.Instant.now()
        workflowRunRepository.save(run)
        log.info("control_plane_workflow_run_cancelled runId={}", runId)
    }

    @Transactional
    override fun markRunCompleted(runId: UUID, outputJson: Map<String, Any?>) {
        val run = workflowRunRepository.findById(runId).orElse(null)
        if (run == null) {
            log.warn("control_plane_mark_run_completed_unknown runId={}", runId)
            return
        }
        if (run.status != STATUS_IN_PROGRESS) {
            log.warn(
                "control_plane_mark_run_completed_skipped runId={} status={}",
                runId,
                run.status,
            )
            return
        }
        run.status = STATUS_COMPLETED
        run.outputJson = outputJson
        run.completedAt = Instant.now()
        run.currentStepKey = null
        workflowRunRepository.save(run)
        auditEventRepository.save(
            AiAuditEventEntity(
                eventType = "WORKFLOW_RUN_COMPLETED",
                actorType = "SYSTEM",
                actorId = null,
                entityType = "AI_WORKFLOW_RUN",
                entityId = runId.toString(),
                action = "COMPLETE",
                detailsJson = mapOf("status" to STATUS_COMPLETED),
            )
        )
        log.info("control_plane_workflow_run_completed runId={}", runId)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun markRunFailed(runId: UUID, errorCode: String?, errorMessage: String?) {
        val run = workflowRunRepository.findById(runId).orElse(null)
        if (run == null) {
            log.warn("control_plane_mark_run_failed_unknown runId={}", runId)
            return
        }
        if (run.status != STATUS_IN_PROGRESS) {
            log.warn(
                "control_plane_mark_run_failed_skipped runId={} status={}",
                runId,
                run.status,
            )
            return
        }
        run.status = STATUS_FAILED
        run.errorCode = errorCode?.take(80)
        run.errorMessage = errorMessage?.take(4000)
        run.failedAt = Instant.now()
        run.completedAt = Instant.now()
        workflowRunRepository.save(run)
        auditEventRepository.save(
            AiAuditEventEntity(
                eventType = "WORKFLOW_RUN_FAILED",
                actorType = "SYSTEM",
                actorId = null,
                entityType = "AI_WORKFLOW_RUN",
                entityId = runId.toString(),
                action = "FAIL",
                detailsJson = mapOf(
                    "errorCode" to errorCode,
                    "messagePreview" to errorMessage?.take(500),
                ),
            )
        )
        log.info(
            "control_plane_workflow_run_failed runId={} errorCode={}",
            runId,
            errorCode,
        )
    }
}
