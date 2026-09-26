package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.UsageMetricsPort
import com.tamixa.application.port.UsageTrackingPort
import com.tamixa.domain.UsageTracking
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class UsageTrackingRepositoryAdapter(
    private val usageTrackingJpaRepository: UsageTrackingJpaRepository,
    private val parentJpaRepository: ParentJpaRepository
) : UsageTrackingPort, UsageMetricsPort {

    override fun sumStoriesForFreePlan(month: String): Long =
        usageTrackingJpaRepository.sumStoriesGeneratedForFreePlan(month)

    override fun sumStoriesForPaidPlan(month: String): Long =
        usageTrackingJpaRepository.sumStoriesGeneratedForPaidPlan(month)

    @Transactional
    override fun getOrCreate(parentId: Long, month: String): UsageTracking {
        usageTrackingJpaRepository.findByParent_IdAndMonth(parentId, month)?.let { return it.toDomain() }
        if (!parentJpaRepository.existsById(parentId)) throw IllegalArgumentException("Parent not found: $parentId")
        usageTrackingJpaRepository.insertIfAbsent(parentId, month)
        val entity = usageTrackingJpaRepository.findByParent_IdAndMonth(parentId, month)
            ?: throw IllegalStateException("usage_tracking row missing after insert for parentId=$parentId month=$month")
        return entity.toDomain()
    }

    @Transactional
    override fun incrementStories(parentId: Long, month: String) {
        getOrCreate(parentId, month)
        val entity = usageTrackingJpaRepository.findByParent_IdAndMonth(parentId, month)
            ?: throw IllegalStateException("UsageTracking entity should exist after getOrCreate for parentId=$parentId month=$month")
        entity.storiesGenerated = entity.storiesGenerated + 1
        entity.updatedAt = Instant.now()
        usageTrackingJpaRepository.save(entity)
    }

    @Transactional
    override fun incrementVoice(parentId: Long, month: String) {
        getOrCreate(parentId, month)
        val entity = usageTrackingJpaRepository.findByParent_IdAndMonth(parentId, month)
            ?: throw IllegalStateException("UsageTracking entity should exist after getOrCreate for parentId=$parentId month=$month")
        entity.voiceGenerations = entity.voiceGenerations + 1
        entity.updatedAt = Instant.now()
        usageTrackingJpaRepository.save(entity)
    }
}

private fun UsageTrackingEntity.toDomain(): UsageTracking = UsageTracking(
    id = id,
    parentId = parent.id,
    month = month,
    storiesGenerated = storiesGenerated,
    voiceGenerations = voiceGenerations,
    createdAt = createdAt,
    updatedAt = updatedAt
)
