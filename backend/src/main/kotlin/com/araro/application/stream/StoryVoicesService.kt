package com.araro.application.stream

import com.araro.application.port.StoryFamilyVoiceRepositoryPort
import com.araro.application.port.VoiceRepositoryPort
import com.araro.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.araro.application.port.StoryTranslationRepositoryPort
import com.araro.application.port.narration.StoryNarrationAudioRepositoryPort
import com.araro.domain.narration.NarrationAudioStatus
import org.springframework.stereotype.Service

/**
 * Lists available voices for a curated story.
 * Used by GET /stories/{id}/voices to expose voice options with premium flags.
 */
@Service
class StoryVoicesService(
    private val translationRepository: StoryTranslationRepositoryPort,
    private val narrationAudioRepository: StoryNarrationAudioRepositoryPort,
    private val voiceCatalog: NarrationVoiceCatalogRepositoryPort,
    private val familyVoiceRepository: StoryFamilyVoiceRepositoryPort,
    private val voiceRepository: VoiceRepositoryPort
) {

    data class VoiceInfo(val voiceProfile: String, val isPremium: Boolean)

    /**
     * Returns voices that have READY audio for the given story + language.
     * When parentId provided, includes "family" if parent has uploaded recording.
     */
    fun getAvailableVoices(storyId: Long, language: String, parentId: Long? = null): List<VoiceInfo> {
        val effectiveLang = language.trim().lowercase().take(10).ifEmpty { "ta" }
        val result = mutableListOf<VoiceInfo>()

        if (parentId != null) {
            if (familyVoiceRepository.existsByStoryIdAndParentIdAndLanguage(storyId, parentId, effectiveLang)) {
                result.add(VoiceInfo(voiceProfile = "family", isPremium = false))
            }
            voiceRepository.findByParentId(parentId)
                .filter { it.elevenlabsVoiceId != null }
                .forEach { result.add(VoiceInfo(voiceProfile = "cloned:${it.id}", isPremium = false)) }
        }

        val translation = translationRepository.findByMasterStoryIdAndLanguage(storyId, effectiveLang)
            ?: return result
        val audios = narrationAudioRepository.findAllByTranslationIdAndStatus(
            translation.id,
            NarrationAudioStatus.READY
        )
        result.addAll(audios.map { audio ->
            val catalog = voiceCatalog.findByToneMode(audio.voiceProfile)
            VoiceInfo(
                voiceProfile = audio.voiceProfile,
                isPremium = catalog?.isPremium ?: false
            )
        })
        return result
    }
}
