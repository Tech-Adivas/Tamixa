package com.tamixa.application.narration.impl

import com.tamixa.application.narration.NarrationTextUtils
import com.tamixa.application.narration.VoiceCloningLanguagePolicy
import com.tamixa.application.narration.VoiceSynthesisStrategy
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
import com.tamixa.application.port.voice.FishAudioVoiceCloningPort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.application.port.voice.GoogleCloudVoiceCloningPort
import com.tamixa.application.port.voice.HeyGenVoiceTtsPort
import com.tamixa.application.port.voice.SelfHostedVoiceCloningPort
import com.tamixa.application.port.voice.VoiceReferenceStoragePort
import com.tamixa.application.voice.referenceAudioExtension
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct
import java.util.concurrent.ConcurrentHashMap

/**
 * Synthesizes speech using parent's cloned voice.
 *
 * When voiceProfile is "cloned:{voiceProfileId}":
 * 0. HeyGen TTS if heygen_voice_id set (voice created in HeyGen app).
 * 1. Google Chirp 3 Instant Custom Voice if google_voice_cloning_key set.
 * 2. Fish Audio if fish_audio_model_id set.
 * 3. ElevenLabs if elevenlabs_voice_id set (legacy).
 * 4. Managed cloud create-from-reference if reference_audio_path (Fish or ElevenLabs by config).
 * 5. XTTS (self-hosted) as fallback for non-Tamil.
 */
@Component
class ClonedVoiceStrategy(
    private val appProperties: AppProperties,
    private val voiceRepository: VoiceRepositoryPort,
    @Autowired(required = false) private val googleCloud: GoogleCloudVoiceCloningPort?,
    @Autowired(required = false) private val elevenLabs: ElevenLabsVoiceCloningPort?,
    @Autowired(required = false) private val fishAudio: FishAudioVoiceCloningPort?,
    @Autowired(required = false) private val heyGenTts: HeyGenVoiceTtsPort?,
    @Autowired(required = false) private val selfHosted: SelfHostedVoiceCloningPort?,
    @Autowired(required = false) private val voiceReferenceStorage: VoiceReferenceStoragePort?
) : VoiceSynthesisStrategy {

    private val log = LoggerFactory.getLogger(javaClass)

    private val preferGooglePrimary: Boolean
        get() = appProperties.voiceCloning.provider.trim().lowercase() == "google"
    private val allowElevenLabsFallback: Boolean
        get() = appProperties.voiceCloning.allowElevenLabsFallback
    private val allowFishAudioFallback: Boolean
        get() = appProperties.voiceCloning.allowFishAudioFallback

    @PostConstruct
    fun logProviderStatus() {
        log.info(
            "Cloned voice strategy: HeyGen={}, Google={}, FishAudio={}, ElevenLabs={}, XTTS={}, voiceRefStorage={}",
            heyGenTts != null,
            googleCloud != null,
            fishAudio != null,
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
        val fishModelId = profile.fishAudioModelId
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

        // 2. Fish Audio cloned model
        if (!fishModelId.isNullOrBlank() && fishAudio != null) {
            log.debug("Cloned voice: Fish Audio synthesize profileId={} modelId={} lang={}", profileId, fishModelId.take(12), language)
            val bytes = fishAudio.synthesize(plainText, fishModelId, language)
            if (bytes != null && bytes.isNotEmpty()) {
                log.info("Cloned voice: Fish Audio synthesis completed profileId={} lang={} bytes={}", profileId, language, bytes.size)
                return bytes
            }
            log.warn("Cloned voice: Fish Audio synthesize returned empty for profileId={}", profileId)
        }

        // 3. Existing ElevenLabs voice (legacy / fallback).
        if (!voiceId.isNullOrBlank() && elevenLabs != null && (!preferGooglePrimary || allowElevenLabsFallback)) {
            log.debug("Cloned voice: ElevenLabs synthesize profileId={} voiceId={} lang={}", profileId, voiceId.take(8), language)
            val bytes = elevenLabs.synthesize(plainText, voiceId, language)
            if (bytes != null && bytes.isNotEmpty()) {
                log.info("Cloned voice: ElevenLabs synthesis completed profileId={} lang={} bytes={}", profileId, language, bytes.size)
                return bytes
            }
            log.warn("Cloned voice: ElevenLabs synthesize returned empty for profileId={}, falling back to XTTS if configured", profileId)
        }

        // 4. No cloud clone id yet: create from reference on first use (ElevenLabs preferred when both Google fallbacks are enabled).
        // Important: do not recreate when an existing id already failed synthesize (voice limits / bad keys).
        if (voiceId.isNullOrBlank() &&
            fishModelId.isNullOrBlank() &&
            !referencePath.isNullOrBlank() &&
            voiceReferenceStorage != null
        ) {
            val p = appProperties.voiceCloning.provider.trim().lowercase()
            val canElevenRef =
                elevenLabs != null && (p == "elevenlabs" || (p == "google" && allowElevenLabsFallback))
            val canFishRef =
                fishAudio != null && (p == "fishaudio" || (p == "google" && allowFishAudioFallback && !canElevenRef))
            log.info(
                "Cloned voice: managed create-from-reference profileId={} path={} lang={} canElevenRef={} canFishRef={}",
                profileId,
                referencePath,
                language,
                canElevenRef,
                canFishRef
            )
            val refBytes = voiceReferenceStorage.getReferenceAudio(referencePath)
            if (refBytes != null && refBytes.isNotEmpty() && (canElevenRef || canFishRef)) {
                createFromReference(profileId, refBytes, referencePath, plainText, language, canElevenRef)
                    ?.let { return it }
            } else if (refBytes == null || refBytes.isEmpty()) {
                if (VoiceCloningLanguagePolicy.requiresElevenLabsForCloned(language)) {
                    log.warn("Cloned voice: Tamil failed — reference audio not found at path={}. Ensure STORAGE_TYPE=s3, S3 is configured, and the file exists in S3. profileId={}", referencePath, profileId)
                } else {
                    log.warn("Cloned voice: could not load reference audio for profileId={} path={}, falling back to XTTS if configured", profileId, referencePath)
                }
            }
        } else if (voiceId.isNullOrBlank() &&
            fishModelId.isNullOrBlank() &&
            !referencePath.isNullOrBlank()
        ) {
            val p = appProperties.voiceCloning.provider.trim().lowercase()
            val canElevenRef =
                elevenLabs != null && (p == "elevenlabs" || (p == "google" && allowElevenLabsFallback))
            val canFishRef =
                fishAudio != null && (p == "fishaudio" || (p == "google" && allowFishAudioFallback && !canElevenRef))
            if (VoiceCloningLanguagePolicy.requiresElevenLabsForCloned(language)) {
                if (preferGooglePrimary && !allowElevenLabsFallback && !allowFishAudioFallback) {
                    log.warn("Cloned voice: Tamil requires google_voice_cloning_key when managed fallbacks are disabled. Complete voice cloning (reference + consent upload, run job). profileId={}", profileId)
                } else if (voiceReferenceStorage == null) {
                    log.warn("Cloned voice: Tamil failed — voiceReferenceStorage missing. Configure S3 reference storage. profileId={}", profileId)
                } else if (!canElevenRef && !canFishRef) {
                    log.warn(
                        "Cloned voice: Tamil failed — no managed clone client for create-from-reference. " +
                            "Set ELEVENLABS_API_KEY and/or FISH_AUDIO_API_KEY and enable the matching Google fallback or use provider=fishaudio/elevenlabs. profileId={}",
                        profileId
                    )
                }
            } else if (!canElevenRef && !canFishRef) {
                log.info(
                    "Cloned voice: skipping managed create-from-reference (elevenLabs={} fishAudio={} voiceRefStorage={}); will use XTTS if configured",
                    elevenLabs != null,
                    fishAudio != null,
                    voiceReferenceStorage != null
                )
            }
        }

        // 5. Fallback: self-hosted XTTS. Never use XTTS for Tamil — it does not support ta (would 500 or wrong language).
        if (VoiceCloningLanguagePolicy.requiresElevenLabsForCloned(language)) {
            if (preferGooglePrimary) {
                log.warn("Cloned voice: Tamil (ta) requires Google key or a managed fallback (ElevenLabs or Fish Audio). XTTS does not support Tamil. profileId={}", profileId)
            } else {
                log.warn("Cloned voice: Tamil (ta) requires a managed cloud clone provider. XTTS does not support Tamil. See above log for why clone paths failed. profileId={}", profileId)
            }
            return null
        }
        if (!referencePath.isNullOrBlank() && selfHosted != null) {
            log.info("Cloned voice: using XTTS profileId={} lang={}", profileId, language)
            return selfHosted.synthesize(plainText, referencePath, language)
        }

        if (googleKey.isNullOrBlank() && voiceId.isNullOrBlank() && fishModelId.isNullOrBlank() && referencePath.isNullOrBlank()) {
            log.warn(
                "Voice profile {} has no google_voice_cloning_key, fish_audio_model_id, elevenlabs_voice_id, or reference_audio_path",
                profileId
            )
        } else if (googleKey != null && googleCloud == null) {
            log.warn("Google voice cloning not configured but google_voice_cloning_key is set for profile {}", profileId)
        } else if (voiceId != null && elevenLabs == null) {
            log.warn("ElevenLabs voice cloning not configured but elevenlabs_voice_id is set for profile {}", profileId)
        } else if (fishModelId != null && fishAudio == null) {
            log.warn("Fish Audio not configured but fish_audio_model_id is set for profile {}", profileId)
        } else if (referencePath != null && selfHosted == null && (elevenLabs == null || voiceReferenceStorage == null) && fishAudio == null) {
            log.warn(
                "Cloned voice not available: profile {} has reference_audio_path but no ElevenLabs+storage, Fish Audio, or XTTS",
                profileId
            )
        }
        return null
    }

    private val createLocks = ConcurrentHashMap<Long, Any>()

    /**
     * First cloned narration of a profile without a cloud voice: creates one from its stored recording (ElevenLabs
     * when [preferElevenLabs], else Fish Audio) and synthesizes [plainText]. Concurrent first plays of the same
     * profile wait here and reuse the voice the first one created, instead of each creating a provider voice (only
     * one id is saved; the others would occupy the account's custom voice slots). A new voice that can't synthesize
     * is deleted again and not saved, as on upload (VoiceCloningService). The lock is per backend instance.
     * @return the audio, or null to fall through to the remaining fallbacks
     */
    private fun createFromReference(
        profileId: Long,
        refBytes: ByteArray,
        referencePath: String,
        plainText: String,
        language: String,
        preferElevenLabs: Boolean
    ): ByteArray? = synchronized(createLocks.computeIfAbsent(profileId) { Any() }) {
        val current = voiceRepository.findById(profileId) ?: return@synchronized null
        val tamil = VoiceCloningLanguagePolicy.requiresElevenLabsForCloned(language)
        val fileName = "reference.${referenceAudioExtension(referencePath)}"

        // Created by a concurrent request (or the upload's cloning job) while this one waited.
        current.elevenlabsVoiceId?.takeIf { it.isNotBlank() && elevenLabs != null }?.let { existing ->
            log.info("Cloned voice: reusing ElevenLabs voice created meanwhile profileId={}", profileId)
            return@synchronized elevenLabs!!.synthesize(plainText, existing, language)?.takeIf { it.isNotEmpty() }
        }
        current.fishAudioModelId?.takeIf { it.isNotBlank() && fishAudio != null }?.let { existing ->
            log.info("Cloned voice: reusing Fish Audio model created meanwhile profileId={}", profileId)
            return@synchronized fishAudio!!.synthesize(plainText, existing, language)?.takeIf { it.isNotEmpty() }
        }

        if (preferElevenLabs) {
            val el = elevenLabs!!
            log.info("Cloned voice: creating ElevenLabs voice from reference profileId={} refBytes={} lang={}", profileId, refBytes.size, language)
            val newVoiceId = el.addVoice(refBytes, fileName, "clone-$profileId")
            if (newVoiceId == null) {
                if (tamil) {
                    log.warn("Cloned voice: Tamil failed — ElevenLabs addVoice failed for profileId={}. Check API key and reference audio format (MP3/WAV).", profileId)
                } else {
                    log.warn("Cloned voice: ElevenLabs addVoice failed for profileId={} (check API key), falling back to XTTS if configured", profileId)
                }
                return@synchronized null
            }
            val bytes = el.synthesize(plainText, newVoiceId, language)
            if (bytes == null || bytes.isEmpty()) {
                el.deleteVoice(newVoiceId)
                log.warn(
                    "Cloned voice: new ElevenLabs voice could not synthesize, deleted it profileId={} lang={}{}",
                    profileId, language, if (tamil) " (Tamil: check ElevenLabs quota and language support)" else ""
                )
                return@synchronized null
            }
            voiceRepository.save(current.copy(elevenlabsVoiceId = newVoiceId))
            log.info("Cloned voice: ElevenLabs voice created and synthesized profileId={} voiceId={} lang={} bytes={}", profileId, newVoiceId.take(8), language, bytes.size)
            bytes
        } else {
            val fish = fishAudio!!
            log.info("Cloned voice: creating Fish Audio model from reference profileId={} refBytes={} lang={}", profileId, refBytes.size, language)
            val newModelId = fish.createModelFromSample(refBytes, fileName, "clone-$profileId")
            if (newModelId == null) {
                if (tamil) {
                    log.warn("Cloned voice: Tamil failed — Fish Audio model creation failed for profileId={}. Check FISH_AUDIO_API_KEY and reference audio.", profileId)
                } else {
                    log.warn("Cloned voice: Fish Audio createModel failed for profileId={} (check API key), falling back to XTTS if configured", profileId)
                }
                return@synchronized null
            }
            val bytes = fish.synthesize(plainText, newModelId, language)
            if (bytes == null || bytes.isEmpty()) {
                fish.deleteModel(newModelId)
                log.warn(
                    "Cloned voice: new Fish Audio model could not synthesize, deleted it profileId={} lang={}{}",
                    profileId, language, if (tamil) " (Tamil: check credits and FISH_AUDIO_API_KEY)" else ""
                )
                return@synchronized null
            }
            voiceRepository.save(current.copy(fishAudioModelId = newModelId))
            log.info("Cloned voice: Fish Audio model created and synthesized profileId={} modelId={} lang={} bytes={}", profileId, newModelId.take(12), language, bytes.size)
            bytes
        }
    }

    companion object {
        private const val CLONED_PREFIX = "cloned:"
    }
}
