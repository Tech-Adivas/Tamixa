package com.araro.application.port

import com.araro.domain.StoryFamilyVoice

interface StoryFamilyVoiceRepositoryPort {
    fun save(voice: StoryFamilyVoice): StoryFamilyVoice
    fun findByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String): StoryFamilyVoice?
    fun findByStoryId(storyId: Long): List<StoryFamilyVoice>
    fun existsByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String): Boolean
    fun deleteByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String)
    fun deleteByStoryId(storyId: Long)
}
