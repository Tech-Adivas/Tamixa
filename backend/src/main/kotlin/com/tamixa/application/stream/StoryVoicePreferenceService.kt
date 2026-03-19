package com.tamixa.application.stream

import com.tamixa.application.port.StoryVoicePreferenceRepositoryPort
import com.tamixa.domain.StoryVoicePreference
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Persists parent's preferred voice per story (e.g. "use my cloned voice for this story").
 * When they open the story, the app can pre-select this voice.
 */
@Service
class StoryVoicePreferenceService(
    private val repository: StoryVoicePreferenceRepositoryPort
) {
    fun getPreference(parentId: Long, storyId: Long, storySource: String): String? =
        repository.find(parentId, storyId, storySource)?.voiceProfile

    fun getPlaybackMode(parentId: Long, storyId: Long, storySource: String): String =
        repository.find(parentId, storyId, storySource)?.playbackMode?.takeIf { it in setOf("default", "my_voice", "avatar") } ?: "default"

    fun setPreference(parentId: Long, storyId: Long, storySource: String, voiceProfile: String, playbackMode: String = "default"): StoryVoicePreference {
        val normalized = voiceProfile.trim().take(100).ifEmpty { "default" }
        val mode = playbackMode.trim().lowercase().take(20).let { if (it in setOf("default", "my_voice", "avatar")) it else "default" }
        val effectiveSource = storySource.trim().take(20).ifEmpty { "library" }
        val existing = repository.find(parentId, storyId, effectiveSource)
        val pref = StoryVoicePreference(
            id = existing?.id ?: 0,
            parentId = parentId,
            storyId = storyId,
            storySource = effectiveSource,
            voiceProfile = normalized,
            playbackMode = mode,
            createdAt = existing?.createdAt ?: Instant.now(),
            updatedAt = Instant.now()
        )
        return repository.save(pref)
    }

    fun clearPreference(parentId: Long, storyId: Long, storySource: String) {
        repository.delete(parentId, storyId, storySource.trim().take(20).ifEmpty { "library" })
    }
}
