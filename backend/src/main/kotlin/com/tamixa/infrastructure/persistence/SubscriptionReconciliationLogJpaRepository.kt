package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface SubscriptionReconciliationLogJpaRepository : JpaRepository<SubscriptionReconciliationLogEntity, Long>
