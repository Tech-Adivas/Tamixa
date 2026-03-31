package com.tamixa.infrastructure.persistence.controlplane

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AiProjectJpaRepository : JpaRepository<AiProjectEntity, UUID> {
    fun findByCode(code: String): AiProjectEntity?
}

interface AiWorkflowJpaRepository : JpaRepository<AiWorkflowEntity, UUID> {
    fun findByWorkflowKey(workflowKey: String): AiWorkflowEntity?
    fun findAllByProject_Id(projectId: UUID): List<AiWorkflowEntity>
}

interface AiWorkflowStepJpaRepository : JpaRepository<AiWorkflowStepEntity, UUID> {
    fun findAllByWorkflow_IdOrderByStepOrderAsc(workflowId: UUID): List<AiWorkflowStepEntity>
}

interface AiPromptAssetJpaRepository : JpaRepository<AiPromptAssetEntity, UUID> {
    fun findAllByProject_Id(projectId: UUID): List<AiPromptAssetEntity>
    fun findByProject_IdAndAssetKey(projectId: UUID, assetKey: String): AiPromptAssetEntity?
}

interface AiPromptAssetVersionJpaRepository : JpaRepository<AiPromptAssetVersionEntity, UUID> {
    fun findByAsset_IdAndVersion(assetId: UUID, version: Int): AiPromptAssetVersionEntity?
    fun findFirstByAsset_IdOrderByVersionDesc(assetId: UUID): AiPromptAssetVersionEntity?
}

interface AiLanguageProfileJpaRepository : JpaRepository<AiLanguageProfileEntity, UUID> {
    fun findByLanguageCode(languageCode: String): AiLanguageProfileEntity?
}

interface AiModelProviderJpaRepository : JpaRepository<AiModelProviderEntity, UUID> {
    fun findByProviderKey(providerKey: String): AiModelProviderEntity?
}

interface AiModelJpaRepository : JpaRepository<AiModelEntity, UUID> {
    fun findByModelKey(modelKey: String): AiModelEntity?
}

interface AiRoutingPolicyJpaRepository : JpaRepository<AiRoutingPolicyEntity, UUID>
interface AiPolicyRuleJpaRepository : JpaRepository<AiPolicyRuleEntity, UUID>
interface AiEvaluationProfileJpaRepository : JpaRepository<AiEvaluationProfileEntity, UUID>

interface AiWorkflowRunJpaRepository : JpaRepository<AiWorkflowRunEntity, UUID> {
    fun findAllByProject_IdOrderByStartedAtDesc(projectId: UUID, pageable: Pageable): Page<AiWorkflowRunEntity>
}

interface AiWorkflowStepRunJpaRepository : JpaRepository<AiWorkflowStepRunEntity, UUID> {
    fun findAllByWorkflowRun_IdOrderByStartedAtAsc(runId: UUID): List<AiWorkflowStepRunEntity>
}

interface AiEvaluationResultJpaRepository : JpaRepository<AiEvaluationResultEntity, UUID>
interface AiHumanReviewQueueJpaRepository : JpaRepository<AiHumanReviewQueueEntity, UUID>
interface AiCostEventJpaRepository : JpaRepository<AiCostEventEntity, UUID>
interface AiAuditEventJpaRepository : JpaRepository<AiAuditEventEntity, UUID> {
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<AiAuditEventEntity>
}
