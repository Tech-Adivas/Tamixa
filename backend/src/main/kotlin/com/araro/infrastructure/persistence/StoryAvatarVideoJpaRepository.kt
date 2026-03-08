package com.araro.infrastructure.persistence

import com.araro.domain.AvatarVideoStatus
import org.springframework.data.jpa.repository.JpaRepository

interface StoryAvatarVideoJpaRepository : JpaRepository<StoryAvatarVideoEntity, Long> {
    fun findByStoryIdAndStorySourceAndParentIdAndLanguageAndVoiceProfile(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String
    ): StoryAvatarVideoEntity?

    fun findAllByStatus(status: AvatarVideoStatus): List<StoryAvatarVideoEntity>
}
