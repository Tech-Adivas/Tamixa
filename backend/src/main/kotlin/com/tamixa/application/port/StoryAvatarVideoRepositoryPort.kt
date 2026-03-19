package com.tamixa.application.port

import com.tamixa.domain.StoryAvatarVideo

interface StoryAvatarVideoRepositoryPort {
    fun findByStoryAndParent(
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String
    ): StoryAvatarVideo?

    fun save(video: StoryAvatarVideo): StoryAvatarVideo

    fun delete(video: StoryAvatarVideo)
}
