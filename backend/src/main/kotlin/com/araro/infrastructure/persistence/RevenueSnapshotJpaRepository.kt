package com.araro.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface RevenueSnapshotJpaRepository : JpaRepository<RevenueSnapshotEntity, Long> {
    fun findBySnapshotDate(date: LocalDate): RevenueSnapshotEntity?
}
