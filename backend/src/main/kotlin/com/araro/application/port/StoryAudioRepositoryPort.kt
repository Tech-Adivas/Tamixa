package com.araro.application.port

import com.araro.domain.StoryAudio

interface StoryAudioRepositoryPort {

    fun save(audio: StoryAudio): StoryAudio

    fun findByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): StoryAudio?

    fun existsByMasterStoryIdAndLanguage(masterStoryId: Long, language: String): Boolean

    fun deleteByMasterStoryIdAndLanguage(masterStoryId: Long, language: String)

    fun deleteByMasterStoryId(masterStoryId: Long)

    /** Returns all story_audio rows (for legacy cleanup; used to fetch S3 paths before delete). */
    fun findAllLegacy(): List<StoryAudio>

    fun deleteAllLegacy(): Int
}
