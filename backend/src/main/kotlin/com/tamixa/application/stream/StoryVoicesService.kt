package com.tamixa.application.stream

import com.tamixa.application.port.StoryFamilyVoiceRepositoryPort
import com.tamixa.application.port.VoiceCloningJobRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.tamixa.application.port.StoryTranslationRepositoryPort
import com.tamixa.application.port.narration.StoryNarrationAudioRepositoryPort
import com.tamixa.domain.VoiceCloningStatus
import com.tamixa.domain.VoiceProfile
import com.tamixa.domain.narration.NarrationAudioStatus
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

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
    private val voiceRepository: VoiceRepositoryPort,
    private val voiceCloningJobRepository: VoiceCloningJobRepositoryPort
) {

    /**
     * Reference paths whose cloning job is still running. Uploads create the profile before its cloud voice exists;
     * offering it meanwhile would let narration create a second cloud voice from the same recording. Jobs older
     * than [IN_FLIGHT_JOB_MAX_AGE] count as stuck (e.g. backend restarted mid-job), so their profile is offered again.
     */
    private fun inFlightReferencePaths(parentId: Long): Set<String> {
        val cutoff = Instant.now().minus(IN_FLIGHT_JOB_MAX_AGE)
        return voiceCloningJobRepository.findByParentId(parentId)
            .filter { (it.status == VoiceCloningStatus.PENDING || it.status == VoiceCloningStatus.PROCESSING) && it.createdAt.isAfter(cutoff) }
            .map { it.audioStoragePath }
            .toSet()
    }

    data class VoiceInfo(val voiceProfile: String, val isPremium: Boolean, val displayLabel: String? = null)

    /**
     * Derives a human-readable label from reference audio path (e.g. "voices/18/dharshik-voice_final.mp3" -> "Dharshik voice").
     */
    private fun displayLabelForCloned(profileId: Long, referencePath: String?): String {
        if (referencePath.isNullOrBlank()) return "My voice $profileId"
        val name = referencePath.substringAfterLast('/').substringBeforeLast('.')
            .replace(Regex("^cloning_\\d+_ref_"), "")
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
            val inFlight = inFlightReferencePaths(parentId)
            voiceRepository.findByParentId(parentId)
                .filter {
                    it.googleVoiceCloningKey != null || it.elevenlabsVoiceId != null ||
                        it.referenceAudioPath != null || it.heygenVoiceId != null
                }
                .filterNot { it.hasNoCloudVoice() && it.referenceAudioPath in inFlight }
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
            // Ready audio is shared per story, but parents' own voices are private: the caller's cloned voices
            // were added above; never list other parents' cloned voices or family recordings here.
            .filterNot { it.voiceProfile.startsWith("cloned:") || it.voiceProfile == "family" }
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

    private fun VoiceProfile.hasNoCloudVoice(): Boolean =
        googleVoiceCloningKey == null && elevenlabsVoiceId == null && fishAudioModelId == null && heygenVoiceId == null

    companion object {
        private val IN_FLIGHT_JOB_MAX_AGE: Duration = Duration.ofMinutes(10)
    }
}
