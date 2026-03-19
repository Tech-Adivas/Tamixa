package com.tamixa.application.analytics

import com.tamixa.infrastructure.persistence.ParentJpaRepository
import com.tamixa.infrastructure.persistence.VoiceCloneAnalyticsEntity
import com.tamixa.infrastructure.persistence.VoiceCloneAnalyticsJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Tracks voice clone creation and usage for analytics and entitlement metrics.
 */
@Service
class VoiceCloneAnalyticsService(
    private val jpaRepository: VoiceCloneAnalyticsJpaRepository,
    private val parentJpaRepository: ParentJpaRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val EVENT_CREATED = "VOICE_CLONE_CREATED"
        const val EVENT_USED = "VOICE_CLONE_USED"
    }

    @Transactional
    fun trackCreated(
        parentId: Long,
        voiceCloningJobId: Long?,
        voiceProfileId: Long?
    ) {
        val parent = parentJpaRepository.findById(parentId).orElse(null) ?: return
        jpaRepository.save(
            VoiceCloneAnalyticsEntity(
                parent = parent,
                eventType = EVENT_CREATED,
                voiceProfileId = voiceProfileId,
                voiceCloningJobId = voiceCloningJobId
            )
        )
        log.debug("Voice clone CREATED tracked parentId={} jobId={} profileId={}", parentId, voiceCloningJobId, voiceProfileId)
    }

    @Transactional
    fun trackUsed(
        parentId: Long,
        voiceProfileId: Long,
        storyId: Long?,
        storySource: String?,
        language: String?
    ) {
        val parent = parentJpaRepository.findById(parentId).orElse(null) ?: return
        jpaRepository.save(
            VoiceCloneAnalyticsEntity(
                parent = parent,
                eventType = EVENT_USED,
                voiceProfileId = voiceProfileId,
                storyId = storyId,
                storySource = storySource,
                language = language
            )
        )
        log.debug("Voice clone USED tracked parentId={} profileId={} storyId={}", parentId, voiceProfileId, storyId)
    }

    @Transactional(readOnly = true)
    fun getMetrics(sinceDays: Int): VoiceCloneMetricsDto {
        val since = java.time.Instant.now().minus(sinceDays.toLong(), java.time.temporal.ChronoUnit.DAYS)
        val created = jpaRepository.countByEventTypeAndCreatedAtAfter(EVENT_CREATED, since)
        val used = jpaRepository.countByEventTypeAndCreatedAtAfter(EVENT_USED, since)
        return VoiceCloneMetricsDto(
            periodDays = sinceDays,
            voiceCloneCreated = created,
            voiceCloneUsed = used
        )
    }
}

data class VoiceCloneMetricsDto(
    val periodDays: Int,
    val voiceCloneCreated: Long,
    val voiceCloneUsed: Long
)
