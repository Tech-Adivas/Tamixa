package com.tamixa.infrastructure.persistence

import com.tamixa.domain.AvatarVideoStatus
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
    fun deleteByParentId(parentId: Long)

    /** Count avatar videos per story (for admin story-wise metrics). */
    @org.springframework.data.jpa.repository.Query("SELECT v.storyId, COUNT(v) FROM StoryAvatarVideoEntity v GROUP BY v.storyId")
    fun countByStoryId(): List<Array<Any>>
}
