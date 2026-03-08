package com.araro.infrastructure.persistence

import com.araro.application.port.SubscriptionReconciliationPort
import com.araro.domain.SubscriptionReconciliationLog
import org.springframework.stereotype.Component

@Component
class SubscriptionReconciliationLogRepositoryAdapter(
    private val subscriptionReconciliationLogJpaRepository: SubscriptionReconciliationLogJpaRepository,
    private val subscriptionJpaRepository: SubscriptionJpaRepository
) : SubscriptionReconciliationPort {

    override fun append(log: SubscriptionReconciliationLog) {
        val sub = subscriptionJpaRepository.findById(log.subscriptionId).orElse(null) ?: return
        val entity = SubscriptionReconciliationLogEntity(
            subscription = sub,
            providerStatus = log.providerStatus,
            dbStatus = log.dbStatus,
            mismatchFlag = log.mismatchFlag,
            checkedAt = log.checkedAt
        )
        subscriptionReconciliationLogJpaRepository.save(entity)
    }
}
