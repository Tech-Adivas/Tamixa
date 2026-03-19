package com.tamixa.application.stream

import com.tamixa.application.port.StoryFamilyVoiceRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.domain.narration.NarrationAudioStatus
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

    data class VoiceInfo(val voiceProfile: String, val isPremium: Boolean, val displayLabel: String? = null)

    /**
     * Derives a human-readable label from reference audio path (e.g. "voices/18/dharshik-voice_final.mp3" -> "Dharshik voice").
     */
    private fun displayLabelForCloned(profileId: Long, referencePath: String?): String {
        if (referencePath.isNullOrBlank()) return "My voice $profileId"
        val name = referencePath.substringAfterLast('/').substringBeforeLast('.')
            .replace('-', ' ').replace('_', ' ')
            .trim().split(' ').joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        return if (name.isNotBlank()) name else "My voice $profileId"
    }

    /**
     * Returns voices that have READY audio for the given story + language.
     * When parentId provided, includes "family" and all cloned profiles with display labels.
     */
    fun getAvailableVoices(storyId: Long, language: String, parentId: Long? = null): List<VoiceInfo> {
        val effectiveLang = StreamLanguageUtils.normalize(language)
        val result = mutableListOf<VoiceInfo>()

        if (parentId != null) {
            if (familyVoiceRepository.existsByStoryIdAndParentIdAndLanguage(storyId, parentId, effectiveLang)) {
                result.add(VoiceInfo(voiceProfile = "family", isPremium = false, displayLabel = "Family recording"))
            }
            voiceRepository.findByParentId(parentId)
                .filter {
                    it.googleVoiceCloningKey != null || it.elevenlabsVoiceId != null ||
                        it.referenceAudioPath != null || it.heygenVoiceId != null
                }
                .forEach {
                    val label = displayLabelForCloned(it.id, it.referenceAudioPath)
                    result.add(VoiceInfo(voiceProfile = "cloned:${it.id}", isPremium = false, displayLabel = label))
                }
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
                isPremium = catalog?.isPremium ?: false,
                displayLabel = null
            )
        })
        return result
    }
}
