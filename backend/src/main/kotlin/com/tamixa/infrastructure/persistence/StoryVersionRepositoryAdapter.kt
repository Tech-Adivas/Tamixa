package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.StoryVersionRepositoryPort
import com.tamixa.domain.StoryVersion
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class StoryVersionRepositoryAdapter(
    private val jpaRepository: StoryVersionJpaRepository
) : StoryVersionRepositoryPort {

    override fun nextVersionNumber(libraryStoryId: Long): Int =
        jpaRepository.nextVersionNumber(libraryStoryId)

    override fun createVersion(
        libraryStoryId: Long,
        title: String?,
        content: String,
        theme: String,
        moral: String?,
        wordCount: Int,
        readingTimeMinutes: Double,
        createdBy: String?
    ): StoryVersion {
        val versionNumber = nextVersionNumber(libraryStoryId)
        val entity = StoryVersionEntity(
            libraryStoryId = libraryStoryId,
            versionNumber = versionNumber,
            title = title,
            content = content,
            theme = theme,
            moral = moral,
            wordCount = wordCount,
            readingTimeMinutes = readingTimeMinutes,
            createdAt = Instant.now(),
            createdBy = createdBy
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }
}
