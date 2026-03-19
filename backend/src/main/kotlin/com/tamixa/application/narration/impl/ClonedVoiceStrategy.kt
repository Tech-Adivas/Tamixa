package com.tamixa.application.narration.impl

import com.tamixa.application.narration.NarrationTextUtils
import com.tamixa.application.narration.VoiceCloningLanguagePolicy
import com.tamixa.application.narration.VoiceSynthesisStrategy
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.application.port.voice.GoogleCloudVoiceCloningPort
import com.tamixa.application.port.voice.HeyGenVoiceTtsPort
import com.tamixa.application.port.voice.SelfHostedVoiceCloningPort
import com.tamixa.application.port.voice.VoiceReferenceStoragePort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

/**
 * Synthesizes speech using parent's cloned voice.
 *
 * When voiceProfile is "cloned:{voiceProfileId}":
 * 0. HeyGen TTS if heygen_voice_id set (voice created in HeyGen app).
 * 1. Google Chirp 3 Instant Custom Voice if google_voice_cloning_key set.
 * 2. ElevenLabs if elevenlabs_voice_id set (legacy).
 * 3. ElevenLabs/Google create-from-reference if reference_audio_path.
 * 4. XTTS (self-hosted) as fallback for non-Tamil.
 */
@Component
class ClonedVoiceStrategy(
    private val appProperties: AppProperties,
    private val voiceRepository: VoiceRepositoryPort,
    @Autowired(required = false) private val googleCloud: GoogleCloudVoiceCloningPort?,
    @Autowired(required = false) private val elevenLabs: ElevenLabsVoiceCloningPort?,
    @Autowired(required = false) private val heyGenTts: HeyGenVoiceTtsPort?,
    @Autowired(required = false) private val selfHosted: SelfHostedVoiceCloningPort?,
    @Autowired(required = false) private val voiceReferenceStorage: VoiceReferenceStoragePort?
) : VoiceSynthesisStrategy {

    private val log = LoggerFactory.getLogger(javaClass)

    /** When true, strategy must not call ElevenLabs (e.g. provider=google). Avoids 401 when only Google is configured. */
    private val useGoogleOnly: Boolean
        get() = appProperties.voiceCloning.provider.trim().lowercase() == "google"

    @PostConstruct
    fun logProviderStatus() {
        log.info(
            "Cloned voice strategy: HeyGen={}, Google={}, ElevenLabs={}, XTTS={}, voiceRefStorage={}",
            heyGenTts != null,
            googleCloud != null,
            elevenLabs != null,
            selfHosted != null,
            voiceReferenceStorage != null
        )
    }

    override fun synthesize(ssml: String, language: String, voiceProfile: String): ByteArray? {
        if (!voiceProfile.startsWith(CLONED_PREFIX)) return null
        val idStr = voiceProfile.removePrefix(CLONED_PREFIX).trim()
        val profileId = idStr.toLongOrNull() ?: return null
        val profile = voiceRepository.findById(profileId) ?: return null
        val raw = ssml.replace(Regex("<[^>]+>"), " ").replace("&amp;", "&")
            .replace("&lt;", "<").replace("&gt;", ">").replace(Regex("\\s+"), " ").trim()
        val plainText = NarrationTextUtils.stripRemainingMarkers(raw)
        if (plainText.isBlank()) return null

        val googleKey = profile.googleVoiceCloningKey
        val voiceId = profile.elevenlabsVoiceId
        val referencePath = profile.referenceAudioPath
        val heygenVoiceId = profile.heygenVoiceId

        // 0. HeyGen voice_id (create voice in HeyGen app, set heygen_voice_id on profile via admin PATCH).
        if (!heygenVoiceId.isNullOrBlank() && heyGenTts != null) {
            log.debug("Cloned voice: HeyGen TTS profileId={} voiceId={} lang={}", profileId, heygenVoiceId.take(8), language)
            val bytes = heyGenTts.synthesize(plainText, heygenVoiceId, language)
            if (bytes != null && bytes.isNotEmpty()) {
                log.info("Cloned voice: HeyGen synthesis completed profileId={} lang={} bytes={}", profileId, language, bytes.size)
                return bytes
            }
            log.warn("Cloned voice: HeyGen synthesize returned empty for profileId={}", profileId)
        }

        // 1. Google Chirp 3 Instant Custom Voice (preferred over ElevenLabs).
        if (!googleKey.isNullOrBlank() && googleCloud != null) {
            log.debug("Cloned voice: Google synthesize profileId={} lang={}", profileId, language)
            val bytes = googleCloud.synthesize(plainText, googleKey, language)
            if (bytes != null && bytes.isNotEmpty()) {
                log.info("Cloned voice: Google synthesis completed profileId={} lang={} bytes={}", profileId, language, bytes.size)
                return bytes
            }
            log.warn("Cloned voice: Google synthesize returned empty for profileId={}", profileId)
        }

        // 2. Existing ElevenLabs voice (legacy). Skip when provider=google to avoid 401.
        if (!useGoogleOnly && !voiceId.isNullOrBlank() && elevenLabs != null) {
            log.debug("Cloned voice: ElevenLabs synthesize profileId={} voiceId={} lang={}", profileId, voiceId.take(8), language)
            val bytes = elevenLabs.synthesize(plainText, voiceId, language)
            if (bytes != null && bytes.isNotEmpty()) {
                log.info("Cloned voice: ElevenLabs synthesis completed profileId={} lang={} bytes={}", profileId, language, bytes.size)
                return bytes
            }
            log.warn("Cloned voice: ElevenLabs synthesize returned empty for profileId={}, falling back to XTTS if configured", profileId)
        }

        // 3. No voice_id yet: create ElevenLabs voice from reference on first use. Skip when provider=google.
        if (!useGoogleOnly && !referencePath.isNullOrBlank() && elevenLabs != null && voiceReferenceStorage != null) {
            log.info("Cloned voice: attempting ElevenLabs (create from reference) profileId={} path={} lang={}", profileId, referencePath, language)
            val refBytes = voiceReferenceStorage.getReferenceAudio(referencePath)
            if (refBytes != null && refBytes.isNotEmpty()) {
                log.info("Cloned voice: creating ElevenLabs voice from reference profileId={} refBytes={} lang={}", profileId, refBytes.size, language)
                val newVoiceId = elevenLabs.addVoice(refBytes, "reference.mp3", "clone-$profileId")
                if (newVoiceId != null) {
                    voiceRepository.save(profile.copy(elevenlabsVoiceId = newVoiceId))
                    log.info("Cloned voice: ElevenLabs voice created profileId={} voiceId={}", profileId, newVoiceId.take(8))
                    val bytes = elevenLabs.synthesize(plainText, newVoiceId, language)
                    if (bytes != null && bytes.isNotEmpty()) {
                        log.info("Cloned voice: ElevenLabs synthesis completed profileId={} lang={} bytes={}", profileId, language, bytes.size)
                        return bytes
                    }
                    if (VoiceCloningLanguagePolicy.requiresElevenLabsForCloned(language)) {
                        log.warn("Cloned voice: Tamil failed — ElevenLabs synthesize returned empty for profileId={}. Check ElevenLabs quota and language support. profileId={}", profileId, profileId)
                    }
                } else {
                    if (VoiceCloningLanguagePolicy.requiresElevenLabsForCloned(language)) {
                        log.warn("Cloned voice: Tamil failed — ElevenLabs addVoice failed for profileId={}. Check API key and reference audio format (MP3/WAV). profileId={}", profileId, profileId)
                    } else {
                        log.warn("Cloned voice: ElevenLabs addVoice failed for profileId={} (check API key), falling back to XTTS if configured", profileId)
                    }
                }
            } else {
                if (VoiceCloningLanguagePolicy.requiresElevenLabsForCloned(language)) {
                    log.warn("Cloned voice: Tamil failed — reference audio not found at path={}. Ensure STORAGE_TYPE=s3, S3 is configured, and the file exists in S3. profileId={}", referencePath, profileId)
                } else {
                    log.warn("Cloned voice: could not load reference audio for profileId={} path={}, falling back to XTTS if configured", profileId, referencePath)
                }
            }
        } else if (!referencePath.isNullOrBlank() && (useGoogleOnly || elevenLabs == null || voiceReferenceStorage == null)) {
            if (VoiceCloningLanguagePolicy.requiresElevenLabsForCloned(language)) {
                if (useGoogleOnly) {
                    log.warn("Cloned voice: Tamil requires google_voice_cloning_key. Complete voice cloning (reference + consent upload, run job); profile has reference only. profileId={}", profileId)
                } else {
                    log.warn("Cloned voice: Tamil failed — ElevenLabs={} voiceReferenceStorage={}. Set ELEVENLABS_API_KEY and STORAGE_TYPE=s3 with S3 credentials so reference audio can be loaded. profileId={}", elevenLabs != null, voiceReferenceStorage != null, profileId)
                }
            } else if (!useGoogleOnly) {
                log.info("Cloned voice: skipping ElevenLabs (elevenLabs={} voiceRefStorage={}); will use XTTS if configured", elevenLabs != null, voiceReferenceStorage != null)
            }
        }

        // 4. Fallback: self-hosted XTTS. Never use XTTS for Tamil — it does not support ta (would 500 or wrong language).
        if (VoiceCloningLanguagePolicy.requiresElevenLabsForCloned(language)) {
            if (useGoogleOnly) {
                log.warn("Cloned voice: Tamil (ta) requires Google voice cloning (google_voice_cloning_key). XTTS does not support Tamil. Complete voice cloning job for this profile. profileId={}", profileId)
            } else {
                log.warn("Cloned voice: Tamil (ta) requires ElevenLabs. XTTS does not support Tamil. See above log for why ElevenLabs path failed. profileId={}", profileId)
            }
            return null
        }
        if (!referencePath.isNullOrBlank() && selfHosted != null) {
            log.info("Cloned voice: using XTTS profileId={} lang={}", profileId, language)
            return selfHosted.synthesize(plainText, referencePath, language)
        }

        if (googleKey.isNullOrBlank() && voiceId.isNullOrBlank() && referencePath.isNullOrBlank()) {
            log.warn("Voice profile {} has no google_voice_cloning_key, elevenlabs_voice_id, or reference_audio_path", profileId)
        } else if (googleKey != null && googleCloud == null) {
            log.warn("Google voice cloning not configured but google_voice_cloning_key is set for profile {}", profileId)
        } else if (voiceId != null && elevenLabs == null) {
            log.warn("ElevenLabs voice cloning not configured but elevenlabs_voice_id is set for profile {}", profileId)
        } else if (referencePath != null && selfHosted == null && (elevenLabs == null || voiceReferenceStorage == null)) {
            log.warn("Cloned voice not available: profile {} has reference_audio_path but ElevenLabs (with reference storage) or XTTS not configured", profileId)
        }
        return null
    }

    companion object {
        private const val CLONED_PREFIX = "cloned:"
    }
}
