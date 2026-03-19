package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.StoryAvatarVideoRepositoryPort
import com.tamixa.domain.AvatarVideoStatus
import com.tamixa.domain.StoryAvatarVideo
import org.springframework.stereotype.Component

@Component
class StoryAvatarVideoRepositoryAdapter(
    private val jpaRepository: StoryAvatarVideoJpaRepository
) : StoryAvatarVideoRepositoryPort {

    override fun findByStoryAndParent(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String
    ): StoryAvatarVideo? =
        jpaRepository.findByStoryIdAndStorySourceAndParentIdAndLanguageAndVoiceProfile(
            storyId, storySource, parentId, language, voiceProfile
        )?.toDomain()

    override fun save(video: StoryAvatarVideo): StoryAvatarVideo {
        val entity = StoryAvatarVideoEntity(
            id = video.id,
            storyId = video.storyId,
            storySource = video.storySource,
            parentId = video.parentId,
            language = video.language,
            voiceProfile = video.voiceProfile,
            storagePath = video.storagePath,
            status = video.status,
            replicatePredictionId = video.replicatePredictionId,
            errorMessage = video.errorMessage,
            createdAt = video.createdAt,
            updatedAt = video.updatedAt
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun delete(video: StoryAvatarVideo) {
        if (video.id != 0L) jpaRepository.deleteById(video.id)
    }
}
