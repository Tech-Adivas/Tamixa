package com.tamixa.application.port

import com.tamixa.domain.StoryFamilyVoice

interface StoryFamilyVoiceRepositoryPort {
    fun save(voice: StoryFamilyVoice): StoryFamilyVoice
    fun findByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String): StoryFamilyVoice?
    fun findByParentIdAndLanguage(parentId: Long, language: String): List<StoryFamilyVoice>
    fun findByStoryId(storyId: Long): List<StoryFamilyVoice>
    fun existsByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String): Boolean
    fun deleteByStoryIdAndParentIdAndLanguage(storyId: Long, parentId: Long, language: String)
    fun deleteByStoryId(storyId: Long)
}
