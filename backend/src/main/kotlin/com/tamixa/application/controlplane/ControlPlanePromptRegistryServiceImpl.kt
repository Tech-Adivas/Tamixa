package com.tamixa.application.controlplane

import com.tamixa.controlplane.application.port.ControlPlanePromptRegistryService
import com.tamixa.controlplane.application.port.PublishPromptVersionCommand
import com.tamixa.controlplane.application.port.PromptAssetVersionView
import com.tamixa.infrastructure.persistence.controlplane.AiPromptAssetEntity
import com.tamixa.infrastructure.persistence.controlplane.AiPromptAssetJpaRepository
import com.tamixa.infrastructure.persistence.controlplane.AiPromptAssetVersionEntity
import com.tamixa.infrastructure.persistence.controlplane.AiPromptAssetVersionJpaRepository
import com.tamixa.infrastructure.persistence.controlplane.AiProjectJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

private const val APPROVAL_DRAFT = "DRAFT"
private const val APPROVAL_APPROVED = "APPROVED"

@Service
class ControlPlanePromptRegistryServiceImpl(
    private val projectRepository: AiProjectJpaRepository,
    private val promptAssetRepository: AiPromptAssetJpaRepository,
    private val promptAssetVersionRepository: AiPromptAssetVersionJpaRepository,
) : ControlPlanePromptRegistryService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun resolve(projectCode: String, assetKey: String, version: Int?): PromptAssetVersionView {
        val project = projectRepository.findByCode(projectCode.trim())
            ?: throw AiControlPlaneEntityNotFoundException("project not found: $projectCode")
        val asset = promptAssetRepository.findByProject_IdAndAssetKey(project.id!!, assetKey.trim())
            ?: throw AiControlPlaneEntityNotFoundException("prompt asset not found: $assetKey")
        val resolvedVersion = version ?: asset.currentVersion
        if (resolvedVersion <= 0) {
            throw AiControlPlaneEntityNotFoundException("no published version for asset: $assetKey")
        }
        val row = promptAssetVersionRepository.findByAsset_IdAndVersion(asset.id!!, resolvedVersion)
            ?: throw AiControlPlaneEntityNotFoundException("prompt version not found: $assetKey@$resolvedVersion")
        return PromptAssetVersionView(
            assetId = asset.id!!,
            assetKey = asset.assetKey,
            version = row.version,
            checksum = row.checksum,
            approvalStatus = row.approvalStatus,
        )
    }

    @Transactional
    override fun publishVersion(command: PublishPromptVersionCommand): PromptAssetVersionView {
        val project = projectRepository.findByCode(command.projectCode.trim())
            ?: throw AiControlPlaneEntityNotFoundException("project not found: ${command.projectCode}")
        val asset = promptAssetRepository.findByProject_IdAndAssetKey(project.id!!, command.assetKey.trim())
            ?: throw AiControlPlaneEntityNotFoundException("prompt asset not found: ${command.assetKey}")
        val nextVersion = (promptAssetVersionRepository.findFirstByAsset_IdOrderByVersionDesc(asset.id!!)?.version ?: 0) + 1
        val checksum = sha256Hex(command.content)
        val saved = promptAssetVersionRepository.save(
            AiPromptAssetVersionEntity(
                asset = asset,
                version = nextVersion,
                content = command.content,
                metadataJson = command.metadata,
                checksum = checksum,
                createdBy = command.createdBy,
                approvalStatus = APPROVAL_DRAFT,
            )
        )
        asset.currentVersion = nextVersion
        asset.updatedAt = Instant.now()
        promptAssetRepository.save(asset)
        log.info(
            "control_plane_prompt_version_published assetKey={} version={} projectCode={}",
            command.assetKey,
            nextVersion,
            command.projectCode,
        )
        return PromptAssetVersionView(
            assetId = asset.id!!,
            assetKey = asset.assetKey,
            version = saved.version,
            checksum = saved.checksum,
            approvalStatus = saved.approvalStatus,
        )
    }

    @Transactional
    override fun approveVersion(assetId: UUID, version: Int, approver: String) {
        val asset = promptAssetRepository.findById(assetId).orElseThrow {
            AiControlPlaneEntityNotFoundException("prompt asset not found: $assetId")
        }
        val row = promptAssetVersionRepository.findByAsset_IdAndVersion(assetId, version)
            ?: throw AiControlPlaneEntityNotFoundException("prompt version not found: $assetId@$version")
        row.approvedBy = approver
        row.approvalStatus = APPROVAL_APPROVED
        promptAssetVersionRepository.save(row)
        asset.updatedAt = Instant.now()
        promptAssetRepository.save(asset)
        log.info("control_plane_prompt_version_approved assetId={} version={}", assetId, version)
    }

    private fun sha256Hex(content: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(content.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
