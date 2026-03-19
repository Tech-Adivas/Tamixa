package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.RevenueSnapshotPort
import com.tamixa.domain.RevenueSnapshot
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class RevenueSnapshotRepositoryAdapter(
    private val revenueSnapshotJpaRepository: RevenueSnapshotJpaRepository
) : RevenueSnapshotPort {

    override fun save(snapshot: RevenueSnapshot): RevenueSnapshot {
        val existing = revenueSnapshotJpaRepository.findBySnapshotDate(snapshot.snapshotDate)
        val entity = existing ?: RevenueSnapshotEntity()
        entity.snapshotDate = snapshot.snapshotDate
        entity.mrrMinor = snapshot.mrrMinor
        entity.activeSubscriptions = snapshot.activeSubscriptions
        entity.churnRate = snapshot.churnRate
        entity.trialConversionRate = snapshot.trialConversionRate
        entity.arpuMinor = snapshot.arpuMinor
        entity.storiesFree = snapshot.storiesFree
        entity.storiesPremium = snapshot.storiesPremium
        if (existing == null) entity.createdAt = snapshot.createdAt
        val saved = revenueSnapshotJpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findByDate(date: LocalDate): RevenueSnapshot? {
        return revenueSnapshotJpaRepository.findBySnapshotDate(date)?.toDomain()
    }
}

private fun RevenueSnapshotEntity.toDomain(): RevenueSnapshot = RevenueSnapshot(
    id = id,
    snapshotDate = snapshotDate,
    mrrMinor = mrrMinor,
    activeSubscriptions = activeSubscriptions,
    churnRate = churnRate,
    trialConversionRate = trialConversionRate,
    arpuMinor = arpuMinor,
    storiesFree = storiesFree,
    storiesPremium = storiesPremium,
    createdAt = createdAt
)
