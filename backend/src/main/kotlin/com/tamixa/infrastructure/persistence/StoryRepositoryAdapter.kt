package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.StoryRepositoryPort
import com.tamixa.domain.Story
import com.tamixa.domain.StoryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StoryRepositoryAdapter(
    private val jpaRepository: StoryJpaRepository,
    private val parentJpaRepository: ParentJpaRepository,
    private val childJpaRepository: ChildJpaRepository,
    private val voiceProfileJpaRepository: VoiceProfileJpaRepository
) : StoryRepositoryPort {

    override fun save(story: Story): Story {
        val parentEntity = parentJpaRepository.findById(story.parentId).orElseThrow {
            IllegalArgumentException("Parent not found: ${story.parentId}")
        }
        val childEntity = story.childId?.let { childJpaRepository.findById(it).orElse(null) }
        val voiceProfileEntity = story.voiceProfileId?.let {
            voiceProfileJpaRepository.findById(it).orElse(null)
        }
        val entity = StoryEntity(
            id = story.id.takeIf { it > 0 } ?: 0,
            parent = parentEntity,
            child = childEntity,
            content = story.content,
            theme = story.theme,
            language = story.language,
            age = story.age,
            childName = story.childName,
            wordCount = story.wordCount,
            readingTimeMinutes = story.readingTimeMinutes,
            title = story.title,
            moral = story.moral,
            status = story.status,
            audioFileUrl = story.audioFileUrl,
            createdAt = story.createdAt,
            safetyScore = story.safetyScore,
            voiceProfile = voiceProfileEntity,
            emotionMode = story.emotionMode,
            parentCustomPrompt = story.parentCustomPrompt,
            coverImageUrl = story.coverImageUrl,
            coverVideoUrl = story.coverVideoUrl
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findById(id: Long): Story? {
        return jpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findByParentId(parentId: Long, pageable: Pageable): Page<Story> {
        return jpaRepository.findByParent_Id(parentId, pageable).map { it.toDomain() }
    }

    override fun countByParentIdAndCreatedAtBetween(parentId: Long, start: Instant, end: Instant): Long {
        return jpaRepository.countByParent_IdAndCreatedAtBetween(parentId, start, end)
    }

    override fun updateStatus(id: Long, status: StoryStatus) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Story not found: $id") }
        entity.status = status
        jpaRepository.save(entity)
    }

    override fun updateContentAndStatus(
        id: Long,
        content: String,
        title: String?,
        moral: String?,
        wordCount: Int,
        readingTimeMinutes: Double,
        status: StoryStatus,
        safetyScore: Int?
    ) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Story not found: $id") }
        entity.content = content
        entity.title = title
        entity.moral = moral
        entity.wordCount = wordCount
        entity.readingTimeMinutes = readingTimeMinutes
        entity.status = status
        entity.safetyScore = safetyScore
        jpaRepository.save(entity)
    }

    override fun updateAudioReady(id: Long, audioFileUrl: String) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Story not found: $id") }
        entity.status = StoryStatus.READY
        entity.audioFileUrl = audioFileUrl
        jpaRepository.save(entity)
    }

    override fun updateNarratedContent(id: Long, content: String, wordCount: Int, readingTimeMinutes: Double) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Story not found: $id") }
        entity.content = content
        entity.wordCount = wordCount
        entity.readingTimeMinutes = readingTimeMinutes
        jpaRepository.save(entity)
    }

    override fun updateCoverImage(id: Long, coverImageUrl: String) {
        updateCover(id, coverImageUrl, null)
    }

    override fun updateCover(id: Long, coverImageUrl: String, coverVideoUrl: String?) {
        val entity = jpaRepository.findById(id).orElseThrow { IllegalArgumentException("Story not found: $id") }
        entity.coverImageUrl = coverImageUrl
        entity.coverVideoUrl = coverVideoUrl
        jpaRepository.save(entity)
    }

    override fun searchByParentId(parentId: Long, query: String, pageable: Pageable): Page<Story> =
        jpaRepository.searchByThemeOrTitle(parentId, query.trim().take(100), pageable).map { it.toDomain() }
}

private fun StoryEntity.toDomain(): Story = Story(
    id = id,
    parentId = parent.id,
    childId = child?.id,
    content = content,
    theme = theme,
    language = language,
    age = age,
    childName = childName,
    wordCount = wordCount,
    readingTimeMinutes = readingTimeMinutes,
    title = title,
    moral = moral,
    status = status,
    audioFileUrl = audioFileUrl,
    createdAt = createdAt,
    safetyScore = safetyScore,
    voiceProfileId = voiceProfile?.id,
    emotionMode = emotionMode,
    parentCustomPrompt = parentCustomPrompt,
    coverImageUrl = coverImageUrl,
    coverVideoUrl = coverVideoUrl
)
