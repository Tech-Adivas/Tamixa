package com.araro.application.playback

import com.araro.infrastructure.persistence.ChildJpaRepository
import com.araro.infrastructure.persistence.ParentJpaRepository
import com.araro.infrastructure.persistence.StoryPlaybackPositionEntity
import com.araro.infrastructure.persistence.StoryPlaybackPositionJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Resume playback: save and retrieve last position per story.
 */
@Service
class PlaybackPositionService(
    private val positionJpaRepository: StoryPlaybackPositionJpaRepository,
    private val parentJpaRepository: ParentJpaRepository,
    private val childJpaRepository: ChildJpaRepository
) {

    @Transactional
    fun savePosition(
        parentId: Long,
        storyId: Long,
        storySource: String,
        positionSeconds: Int,
        childId: Long? = null
    ) {
        val parent = parentJpaRepository.findById(parentId).orElse(null) ?: return
        val child = childId?.let { childJpaRepository.findById(it).orElse(null) }?.takeIf { it.parent.id == parentId }
        val source = if (storySource.lowercase() == "curated") "curated" else "generated"
        val existing = positionJpaRepository.findByParent_IdAndStoryIdAndStorySource(parentId, storyId, source)
        if (existing != null) {
            existing.positionSeconds = positionSeconds.coerceAtLeast(0)
            existing.updatedAt = java.time.Instant.now()
            positionJpaRepository.save(existing)
        } else {
            positionJpaRepository.save(
                StoryPlaybackPositionEntity(
                    parent = parent,
                    storyId = storyId,
                    storySource = source,
                    positionSeconds = positionSeconds.coerceAtLeast(0),
                    child = child
                )
            )
        }
    }

    @Transactional(readOnly = true)
    fun getPosition(parentId: Long, storyId: Long, storySource: String): Int? {
        val source = if (storySource.lowercase() == "curated") "curated" else "generated"
        return positionJpaRepository.findByParent_IdAndStoryIdAndStorySource(parentId, storyId, source)
            ?.positionSeconds
    }

    @Transactional(readOnly = true)
    fun getRecentWithPositions(parentId: Long, limit: Int = 10): List<PlaybackPositionDto> {
        val page = positionJpaRepository.findByParent_IdOrderByUpdatedAtDesc(
            parentId,
            PageRequest.of(0, limit.coerceIn(1, 50))
        )
        return page.map {
            PlaybackPositionDto(
                storyId = it.storyId,
                storySource = it.storySource,
                positionSeconds = it.positionSeconds,
                updatedAt = it.updatedAt
            )
        }
    }
}

data class PlaybackPositionDto(
    val storyId: Long,
    val storySource: String,
    val positionSeconds: Int,
    val updatedAt: java.time.Instant
)
