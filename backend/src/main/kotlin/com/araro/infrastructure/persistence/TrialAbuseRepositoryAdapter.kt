package com.araro.infrastructure.persistence

import com.araro.application.port.TrialAbusePort
import org.springframework.stereotype.Component

@Component
class TrialAbuseRepositoryAdapter(
    private val trialUsageJpaRepository: TrialUsageJpaRepository,
    private val subscriptionJpaRepository: SubscriptionJpaRepository,
    private val parentJpaRepository: ParentJpaRepository
) : TrialAbusePort {

    override fun hasTrialBeenUsed(deviceHash: String, emailHash: String): Boolean =
        trialUsageJpaRepository.existsByDeviceHashAndEmailHash(deviceHash, emailHash)

    override fun hasParentUsedTrial(parentId: Long): Boolean {
        val sub = subscriptionJpaRepository.findByParent_Id(parentId) ?: return false
        return sub.trialUsed
    }

    override fun recordTrialUsage(parentId: Long, deviceHash: String, emailHash: String) {
        val parent = parentJpaRepository.findById(parentId).orElse(null) ?: return
        if (!trialUsageJpaRepository.existsByDeviceHashAndEmailHash(deviceHash, emailHash)) {
            trialUsageJpaRepository.save(
                TrialUsageEntity(
                    parent = parent,
                    deviceHash = deviceHash,
                    emailHash = emailHash
                )
            )
        }
    }
}
