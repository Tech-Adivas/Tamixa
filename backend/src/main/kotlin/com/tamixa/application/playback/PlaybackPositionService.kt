package com.tamixa.application.playback

import com.tamixa.application.port.StoryLibraryRepositoryPort
import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.persistence.ChildJpaRepository
import com.tamixa.infrastructure.persistence.ParentJpaRepository
import com.tamixa.infrastructure.persistence.StoryPlaybackPositionEntity
import com.tamixa.infrastructure.persistence.StoryPlaybackPositionJpaRepository
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
    private val childJpaRepository: ChildJpaRepository,
    private val storyLibraryRepository: StoryLibraryRepositoryPort,
    private val storyRepository: StoryRepositoryPort,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    private val appProperties: AppProperties
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
        val source = if (storySource.lowercase() == "library") "library" else "generated"
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
        val source = if (storySource.lowercase() == "library") "library" else "generated"
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

    /**
     * Returns recent playback positions enriched with story title, cover, and progress.
     * Used for "Continue Adventure" home section.
     */
    @Transactional(readOnly = true)
    fun getRecentEnriched(parentId: Long, limit: Int = 10): List<PlaybackPositionEnrichedDto> {
        val positions = getRecentWithPositions(parentId, limit)
        val baseUrl = appProperties.audio.publicBaseUrl.trimEnd('/')
        return positions.map { pos ->
            val (title, coverPath, durationSeconds) = when (pos.storySource) {
                "library" -> {
                    val story = storyLibraryRepository.findById(pos.storyId)
                    Triple(
                        story?.title ?: story?.theme ?: "Story",
                        story?.coverImageUrl?.let { coverImageUrlResolver.resolveCoverPath(it) },
                        (story?.readingTimeMinutes ?: 0.0).toInt() * 60
                    )
                }
                else -> {
                    val story = storyRepository.findById(pos.storyId)
                    Triple(
                        story?.title ?: story?.theme ?: "Story",
                        story?.coverImageUrl?.let { coverImageUrlResolver.resolveCoverPath(it) },
                        (story?.readingTimeMinutes ?: 0.0).toInt() * 60
                    )
                }
            }
            val progress = if (durationSeconds > 0) (pos.positionSeconds.toDouble() / durationSeconds).coerceIn(0.0, 1.0) else null
            val coverImageUrl = coverPath?.let { if (it.startsWith("http")) it else "$baseUrl$it" }
            PlaybackPositionEnrichedDto(
                storyId = pos.storyId,
                storySource = pos.storySource,
                positionSeconds = pos.positionSeconds,
                updatedAt = pos.updatedAt,
                title = title,
                coverImageUrl = coverImageUrl,
                progress = progress
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

data class PlaybackPositionEnrichedDto(
    val storyId: Long,
    val storySource: String,
    val positionSeconds: Int,
    val updatedAt: java.time.Instant,
    val title: String,
    val coverImageUrl: String?,
    val progress: Double?
)
