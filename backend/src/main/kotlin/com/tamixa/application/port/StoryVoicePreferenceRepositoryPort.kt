package com.tamixa.application.port

import com.tamixa.domain.StoryVoicePreference

interface StoryVoicePreferenceRepositoryPort {
    fun find(parentId: Long, storyId: Long, storySource: String): StoryVoicePreference?
    fun save(preference: StoryVoicePreference): StoryVoicePreference
    fun delete(parentId: Long, storyId: Long, storySource: String)
}
