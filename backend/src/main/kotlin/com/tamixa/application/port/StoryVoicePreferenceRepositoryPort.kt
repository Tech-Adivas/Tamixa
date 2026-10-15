package com.tamixa.application.port

import com.tamixa.domain.StoryVoicePreference

interface StoryVoicePreferenceRepositoryPort {
    fun find(parentId: Long, storyId: Long, storySource: String): StoryVoicePreference?
    fun save(preference: StoryVoicePreference): StoryVoicePreference
    fun delete(parentId: Long, storyId: Long, storySource: String)
    /** Removes every story's "play in this voice" setting for [voiceProfiles] (e.g. a deleted "cloned:12"). */
    fun deleteByVoiceProfileIn(voiceProfiles: Collection<String>)
    /** Distinct voice profiles in use that start with [prefix] (e.g. "cloned:"), for orphan cleanup. */
    fun findDistinctVoiceProfilesStartingWith(prefix: String): List<String>
}
