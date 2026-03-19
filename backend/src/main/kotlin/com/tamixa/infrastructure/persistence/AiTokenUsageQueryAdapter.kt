package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.AiTokenUsageQueryPort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class AiTokenUsageQueryAdapter(
    private val tokenUsageJpaRepository: StoryTokenUsageJpaRepository
) : AiTokenUsageQueryPort {

    override fun getTokensUsedByParent(parentId: Long, start: Instant, end: Instant): Long =
        tokenUsageJpaRepository.sumTotalTokensByParentAndDateRange(parentId, start, end)

    override fun getTokensUsedSystemWide(start: Instant, end: Instant): Long =
        tokenUsageJpaRepository.sumTotalTokensByDateRange(start, end)
}
