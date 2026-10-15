package com.tamixa.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface StoryVoicePreferenceJpaRepository : JpaRepository<StoryVoicePreferenceEntity, Long> {
    fun findByParent_IdAndStoryIdAndStorySource(
        parentId: Long,
        storyId: Long,
        storySource: String
    ): StoryVoicePreferenceEntity?

    fun deleteByParent_IdAndStoryIdAndStorySource(parentId: Long, storyId: Long, storySource: String)

    fun deleteByVoiceProfileIn(voiceProfiles: Collection<String>)

    @org.springframework.data.jpa.repository.Query(
        "select distinct p.voiceProfile from StoryVoicePreferenceEntity p where p.voiceProfile like concat(:prefix, '%')"
    )
    fun findDistinctVoiceProfilesStartingWith(@org.springframework.data.repository.query.Param("prefix") prefix: String): List<String>
}
