package com.tamixa.application.controlplane

import java.util.UUID

data class WorkflowRunSummaryView(
    val id: UUID,
    val workflowKey: String,
    val status: String,
    val currentStepKey: String?,
    val entityType: String,
    val entityId: String?,
    val startedAtIso: String,
)

data class PagedWorkflowRunSummaries(
    val content: List<WorkflowRunSummaryView>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean,
)
