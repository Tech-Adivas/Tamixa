package com.araro.application.port

import com.araro.domain.StoryAvatarVideo

interface StoryAvatarVideoRepositoryPort {
    fun findByStoryAndParent(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String
    ): StoryAvatarVideo?

    fun save(video: StoryAvatarVideo): StoryAvatarVideo
}
