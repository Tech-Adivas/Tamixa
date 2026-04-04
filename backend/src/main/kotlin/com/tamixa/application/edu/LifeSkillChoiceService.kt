package com.tamixa.application.edu

import com.tamixa.api.edu.dto.LifeSkillCountersResponse
import com.tamixa.infrastructure.persistence.ChildJpaRepository
import com.tamixa.infrastructure.persistence.ChildLifeSkillSoftCounterEntity
import com.tamixa.infrastructure.persistence.ChildLifeSkillSoftCounterJpaRepository
import com.tamixa.infrastructure.persistence.LifeSkillChoiceEventEntity
import com.tamixa.infrastructure.persistence.LifeSkillChoiceEventJpaRepository
import com.tamixa.infrastructure.persistence.ParentJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class LifeSkillChoiceService(
    private val parentJpaRepository: ParentJpaRepository,
    private val childJpaRepository: ChildJpaRepository,
    private val lifeSkillChoiceEventJpaRepository: LifeSkillChoiceEventJpaRepository,
    private val childLifeSkillSoftCounterJpaRepository: ChildLifeSkillSoftCounterJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val PILLAR_MIN = -100_000
        private const val PILLAR_MAX = 100_000
    }

    @Transactional
    fun recordChoice(
        parentEmail: String,
        libraryStoryId: Long,
        childId: Long,
        segmentId: String,
        choiceId: String,
        skillDeltas: Map<String, Int>?,
    ) {
        val parent = parentJpaRepository.findByEmail(parentEmail)
            ?: throw IllegalArgumentException("Parent not found")
        val child = childJpaRepository.findByIdAndParent_Id(childId, parent.id)
            ?: throw IllegalArgumentException("Child not found or does not belong to this account")
        val normalizedSegment = segmentId.trim().take(128)
        val normalizedChoice = choiceId.trim().take(128)
        if (normalizedSegment.isBlank() || normalizedChoice.isBlank()) {
            throw IllegalArgumentException("segmentId and choiceId are required")
        }
        val deltas = skillDeltas.orEmpty()
            .mapNotNull { (k, v) ->
                val key = k.trim().take(64)
                if (key.isBlank()) return@mapNotNull null
                key to v.coerceIn(-50, 50)
            }
            .take(32)
            .toMap()
        val entity = LifeSkillChoiceEventEntity(
            parentId = parent.id,
            childId = child.id,
            libraryStoryId = libraryStoryId,
            segmentId = normalizedSegment,
            choiceId = normalizedChoice,
            skillDeltas = deltas,
        )
        lifeSkillChoiceEventJpaRepository.save(entity)
        applyPillarDeltas(childId, deltas)
        log.info(
            "Life skill choice recorded parentId={} childId={} libraryStoryId={} segmentId={} choiceId={} deltaKeys={}",
            parent.id,
            childId,
            libraryStoryId,
            normalizedSegment,
            normalizedChoice,
            deltas.keys.joinToString(",")
        )
    }

    fun getSoftCounters(parentEmail: String, childId: Long): LifeSkillCountersResponse {
        val parent = parentJpaRepository.findByEmail(parentEmail)
            ?: throw IllegalArgumentException("Parent not found")
        childJpaRepository.findByIdAndParent_Id(childId, parent.id)
            ?: throw IllegalArgumentException("Child not found or does not belong to this account")
        val row = childLifeSkillSoftCounterJpaRepository.findById(childId).orElseGet {
            ChildLifeSkillSoftCounterEntity(childId = childId, wisdom = 0, social = 0, money = 0, balance = 0)
        }
        return LifeSkillCountersResponse(
            childId = childId,
            wisdom = row.wisdom,
            social = row.social,
            money = row.money,
            balance = row.balance,
        )
    }

    private fun applyPillarDeltas(childId: Long, deltas: Map<String, Int>) {
        if (deltas.isEmpty()) return
        val folded = LifeSkillPillarNormalization.foldToPillars(deltas)
        if (folded.isEmpty()) return
        val existing = childLifeSkillSoftCounterJpaRepository.findById(childId).orElse(null)
        val base = existing ?: ChildLifeSkillSoftCounterEntity(childId = childId)
        var w = base.wisdom
        var s = base.social
        var m = base.money
        var b = base.balance
        folded.forEach { (pillar, delta) ->
            when (pillar) {
                LifeSkillPillar.WISDOM -> w += delta
                LifeSkillPillar.SOCIAL -> s += delta
                LifeSkillPillar.MONEY -> m += delta
                LifeSkillPillar.BALANCE -> b += delta
            }
        }
        base.wisdom = w.coerceIn(PILLAR_MIN, PILLAR_MAX)
        base.social = s.coerceIn(PILLAR_MIN, PILLAR_MAX)
        base.money = m.coerceIn(PILLAR_MIN, PILLAR_MAX)
        base.balance = b.coerceIn(PILLAR_MIN, PILLAR_MAX)
        base.updatedAt = Instant.now()
        childLifeSkillSoftCounterJpaRepository.save(base)
    }
}
