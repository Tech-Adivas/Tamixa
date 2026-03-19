package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.SubscriptionReconciliationPort
import com.tamixa.domain.SubscriptionReconciliationLog
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
