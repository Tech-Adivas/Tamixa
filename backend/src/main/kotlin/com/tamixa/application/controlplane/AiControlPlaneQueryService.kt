package com.tamixa.application.controlplane

import com.tamixa.infrastructure.persistence.controlplane.AiProjectJpaRepository
import com.tamixa.infrastructure.persistence.controlplane.AiWorkflowRunJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AiControlPlaneQueryService(
    private val projectRepository: AiProjectJpaRepository,
    private val workflowRunRepository: AiWorkflowRunJpaRepository,
) {

    @Transactional(readOnly = true)
    fun listWorkflowRuns(projectCode: String, page: Int, size: Int): PagedWorkflowRunSummaries {
        val project = projectRepository.findByCode(projectCode.trim())
            ?: throw AiControlPlaneEntityNotFoundException("project not found: $projectCode")
        val pg = workflowRunRepository.findAllByProject_IdOrderByStartedAtDesc(
            project.id!!,
            PageRequest.of(page, size),
        )
        val content = pg.content.map { run ->
            WorkflowRunSummaryView(
                id = run.id!!,
                workflowKey = run.workflow.workflowKey,
                status = run.status,
                currentStepKey = run.currentStepKey,
                entityType = run.entityType,
                entityId = run.entityId,
                startedAtIso = run.startedAt.toString(),
            )
        }
        return PagedWorkflowRunSummaries(
            content = content,
            page = pg.number,
            size = pg.size,
            totalElements = pg.totalElements,
            totalPages = pg.totalPages,
            first = pg.isFirst,
            last = pg.isLast,
        )
    }
}
