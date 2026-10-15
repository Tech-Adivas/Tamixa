package com.tamixa.application.voice

import com.tamixa.application.analytics.VoiceCloneAnalyticsService
import com.tamixa.application.port.VoiceCloningJobRepositoryPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
import com.tamixa.application.port.voice.FishAudioVoiceCloningPort
import com.tamixa.application.port.voice.GoogleCloudVoiceCloningPort
import com.tamixa.application.port.voice.VoiceReferenceStoragePort
import com.tamixa.domain.VoiceProfile
import com.tamixa.domain.VoiceCloningJob
import com.tamixa.domain.VoiceCloningStatus
import com.tamixa.infrastructure.config.AppProperties
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.Instant

@Service
class VoiceCloningService(
    private val voiceCloningJobRepository: VoiceCloningJobRepositoryPort,
    private val parentRepository: ParentRepositoryPort,
    private val voiceRepository: VoiceRepositoryPort,
    private val voiceReferenceStorage: VoiceReferenceStoragePort,
    @Autowired(required = false) private val elevenLabsAdapter: ElevenLabsVoiceCloningPort?,
    @Autowired(required = false) private val fishAudioAdapter: FishAudioVoiceCloningPort?,
    @Autowired(required = false) private val googleCloudAdapter: GoogleCloudVoiceCloningPort?,
    private val appProperties: AppProperties,
    @Autowired(required = false) private val voiceCloneAnalytics: VoiceCloneAnalyticsService?,
    @Autowired(required = false) private val transactionTemplate: org.springframework.transaction.support.TransactionTemplate? = null,
    @Autowired(required = false) private val clonedVoiceMedia: ClonedVoiceMediaService? = null
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val voiceCloningConfig get() = appProperties.voiceCloning

    companion object {
        const val MAX_CLONED_VOICES_PER_PARENT = 5
    }

    private fun checkMaxClonedVoices(parentId: Long) {
        val count = voiceRepository.findByParentId(parentId).size
        if (count >= MAX_CLONED_VOICES_PER_PARENT) {
            throw MaxClonedVoicesReachedException(
                "Maximum $MAX_CLONED_VOICES_PER_PARENT cloned voices per profile. Delete one to add another."
            )
        }
    }

    @Transactional
    fun createVoiceCloningJob(
        parentId: Long,
        audioStoragePath: String,
        consentAudioStoragePath: String?,
        audioFileSizeBytes: Long,
        voiceName: String
    ): VoiceCloningJob {
        val job = VoiceCloningJob(
            id = 0,
            parentId = parentId,
            audioStoragePath = audioStoragePath,
            consentAudioStoragePath = consentAudioStoragePath,
            audioFileSizeBytes = audioFileSizeBytes,
            voiceName = voiceName,
            elevenLabsVoiceId = null,
            fishAudioModelId = null,
            googleVoiceCloningKey = null,
            status = VoiceCloningStatus.PENDING,
            errorMessage = null,
            createdAt = Instant.now(),
            completedAt = null
        )
        return voiceCloningJobRepository.save(job)
    }

    /**
     * For POST /voice-cloning (a parent asks to clone one of their recordings). The recording must belong to one of
     * the parent's voice profiles that has no cloud clone yet and no clone in progress, so repeated calls can't
     * create extra paid provider voices or get around the voice limit. Serialized per parent with uploads.
     * Callers start the returned job with [processVoiceCloningJob].
     */
    fun requestCloneOfRecording(
        parentId: Long,
        audioStoragePath: String,
        consentAudioStoragePath: String?,
        audioFileSizeBytes: Long,
        voiceName: String
    ): VoiceCloningJob = synchronized(uploadLock(parentId)) {
        val profile = voiceRepository.findByParentId(parentId).firstOrNull { it.referenceAudioPath == audioStoragePath }
            ?: throw IllegalArgumentException("No voice recording found at this path. Upload it first via POST /voice/upload")
        if (!profile.elevenlabsVoiceId.isNullOrBlank() || !profile.fishAudioModelId.isNullOrBlank() ||
            !profile.googleVoiceCloningKey.isNullOrBlank()
        ) {
            throw VoiceAlreadyClonedException("This voice is already cloned")
        }
        val inProgress = voiceCloningJobRepository.findByParentId(parentId).any {
            it.audioStoragePath == audioStoragePath &&
                (it.status == VoiceCloningStatus.PENDING || it.status == VoiceCloningStatus.PROCESSING)
        }
        if (inProgress) throw VoiceAlreadyClonedException("This voice is already being cloned")
        log.info("Clone requested for voice profileId={} parentId={}", profile.id, parentId)
        createVoiceCloningJob(parentId, audioStoragePath, consentAudioStoragePath, audioFileSizeBytes, voiceName)
    }

    @Async
    @Transactional
    fun processVoiceCloningJob(jobId: Long) {
        val job = voiceCloningJobRepository.findById(jobId)
            ?: throw VoiceCloningJobNotFoundException(jobId)

        if (job.status != VoiceCloningStatus.PENDING) {
            log.warn("Job already processed: jobId={}", jobId)
            return
        }

        val provider = voiceCloningConfig.provider.trim().lowercase()
        val useGooglePrimary = provider == "google" && googleCloudAdapter != null
        val useElevenLabsPrimary = provider == "elevenlabs" && voiceCloningConfig.elevenLabsApiKey.isNotBlank() && elevenLabsAdapter != null
        val useFishPrimary = provider == "fishaudio" && voiceCloningConfig.fishAudioApiKey.isNotBlank() && fishAudioAdapter != null
        val canFallbackToElevenLabsFromGoogle = useGooglePrimary &&
            voiceCloningConfig.allowElevenLabsFallback &&
            voiceCloningConfig.elevenLabsApiKey.isNotBlank() &&
            elevenLabsAdapter != null
        val canFallbackToFishFromGoogle = useGooglePrimary &&
            voiceCloningConfig.allowFishAudioFallback &&
            voiceCloningConfig.fishAudioApiKey.isNotBlank() &&
            fishAudioAdapter != null

        if (!voiceCloningConfig.enabled || (!useGooglePrimary && !useElevenLabsPrimary && !useFishPrimary)) {
            val msg =
                "Set VOICE_CLONING_ENABLED=true and VOICE_CLONING_PROVIDER=google (with GOOGLE_CLOUD_TTS_API_KEY), " +
                    "or provider=elevenlabs (with ELEVENLABS_API_KEY), or provider=fishaudio (with FISH_AUDIO_API_KEY)"
            log.error(
                "Voice cloning job cannot be processed: jobId={} provider={}",
                jobId,
                voiceCloningConfig.provider,
                StructuredArguments.kv("parentId", job.parentId)
            )
            voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.FAILED, msg, null, null, null)
            return
        }

        voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.PROCESSING, null, null, null, null)

        try {
            when {
                useGooglePrimary -> {
                    val hasConsentAudio = !job.consentAudioStoragePath.isNullOrBlank()
                    if (!hasConsentAudio && canFallbackToElevenLabsFromGoogle) {
                        log.info(
                            "Google is primary but consent audio is missing; using ElevenLabs fallback directly. jobId={} parentId={}",
                            jobId,
                            job.parentId
                        )
                        processWithElevenLabs(jobId, job)
                    } else if (!hasConsentAudio && canFallbackToFishFromGoogle) {
                        log.info(
                            "Google is primary but consent audio is missing; using Fish Audio fallback directly. jobId={} parentId={}",
                            jobId,
                            job.parentId
                        )
                        processWithFishAudio(jobId, job)
                    } else {
                        try {
                            processWithGoogle(jobId, job)
                        } catch (googleError: Exception) {
                            if (canFallbackToElevenLabsFromGoogle) {
                                log.warn(
                                    "Google voice cloning failed; trying ElevenLabs fallback. jobId={} parentId={} reason={}",
                                    jobId,
                                    job.parentId,
                                    googleError.message
                                )
                                try {
                                    processWithElevenLabs(jobId, job)
                                    log.info(
                                        "Voice cloning completed with ElevenLabs fallback after Google failure: jobId={} parentId={}",
                                        jobId,
                                        job.parentId
                                    )
                                } catch (fallbackError: Exception) {
                                    if (canFallbackToFishFromGoogle) {
                                        log.warn(
                                            "ElevenLabs fallback failed; trying Fish Audio. jobId={} parentId={} reason={}",
                                            jobId,
                                            job.parentId,
                                            fallbackError.message
                                        )
                                        try {
                                            processWithFishAudio(jobId, job)
                                            log.info(
                                                "Voice cloning completed with Fish Audio after Google+ElevenLabs failure: jobId={} parentId={}",
                                                jobId,
                                                job.parentId
                                            )
                                        } catch (fishError: Exception) {
                                            throw IllegalStateException(
                                                "Google clone failed: ${googleError.message ?: "unknown"}; " +
                                                    "ElevenLabs fallback failed: ${fallbackError.message ?: "unknown"}; " +
                                                    "Fish Audio fallback failed: ${fishError.message ?: "unknown"}",
                                                fishError
                                            )
                                        }
                                    } else {
                                        throw IllegalStateException(
                                            "Google clone failed: ${googleError.message ?: "unknown"}; " +
                                                "ElevenLabs fallback failed: ${fallbackError.message ?: "unknown"}",
                                            fallbackError
                                        )
                                    }
                                }
                            } else if (canFallbackToFishFromGoogle) {
                                log.warn(
                                    "Google voice cloning failed; trying Fish Audio fallback. jobId={} parentId={} reason={}",
                                    jobId,
                                    job.parentId,
                                    googleError.message
                                )
                                try {
                                    processWithFishAudio(jobId, job)
                                    log.info(
                                        "Voice cloning completed with Fish Audio fallback after Google failure: jobId={} parentId={}",
                                        jobId,
                                        job.parentId
                                    )
                                } catch (fishError: Exception) {
                                    throw IllegalStateException(
                                        "Google clone failed: ${googleError.message ?: "unknown"}; " +
                                            "Fish Audio fallback failed: ${fishError.message ?: "unknown"}",
                                        fishError
                                    )
                                }
                            } else {
                                throw googleError
                            }
                        }
                    }
                }
                useElevenLabsPrimary -> processWithElevenLabs(jobId, job)
                useFishPrimary -> processWithFishAudio(jobId, job)
                else -> throw IllegalStateException("No voice cloning provider")
            }
        } catch (e: Exception) {
            log.error("Voice cloning failed: jobId={}", jobId, e)
            voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.FAILED, e.message ?: "Unknown error", null, null, null)
        }
    }

    /**
     * The voice profile [job] clones: the one whose recording it is (every job is created for an existing profile).
     * Null when that voice was deleted while it was being cloned; the job is then marked FAILED and the caller
     * discards the new provider voice, instead of bringing the deleted voice back as a new profile.
     */
    private fun profileOfJob(jobId: Long, job: VoiceCloningJob, profiles: List<VoiceProfile>): VoiceProfile? {
        profiles.firstOrNull { it.referenceAudioPath == job.audioStoragePath }?.let { return it }
        log.info("Voice deleted while cloning; discarding the new clone jobId={} parentId={}", jobId, job.parentId)
        voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.FAILED, "Voice was deleted while cloning", null, null, null)
        return null
    }

    private fun processWithGoogle(jobId: Long, job: VoiceCloningJob) {
        val consentPath = job.consentAudioStoragePath
        if (consentPath.isNullOrBlank()) {
            throw IllegalStateException("Google voice cloning requires consent audio. Upload both reference and consent files.")
        }
        val referenceBytes = voiceReferenceStorage.getReferenceAudio(job.audioStoragePath)
            ?: throw IllegalStateException("Reference audio not found: ${job.audioStoragePath}")
        val consentBytes = voiceReferenceStorage.getReferenceAudio(consentPath)
            ?: throw IllegalStateException("Consent audio not found: $consentPath")
        if (referenceBytes.isEmpty() || consentBytes.isEmpty()) {
            throw IllegalStateException("Reference and consent audio must not be empty")
        }

        val languageCode = "en-US" // Default; can add language to job later
        val adapter = googleCloudAdapter
            ?: throw IllegalStateException("Google Cloud adapter not configured. Set GOOGLE_CLOUD_TTS_API_KEY.")
        val voiceCloningKey = adapter.createVoiceCloningKey(referenceBytes, consentBytes, languageCode)
            ?: throw IllegalStateException("Google voice cloning key creation failed")

        // Google keys are not stored resources at Google: nothing to delete when the voice is gone.
        val existing = profileOfJob(jobId, job, voiceRepository.findByParentId(job.parentId)) ?: return
        voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.READY, null, null, voiceCloningKey, null)
        log.debug("Updating VoiceProfile id={} with Google key for parentId={}", existing.id, job.parentId)
        val profile = voiceRepository.save(existing.copy(googleVoiceCloningKey = voiceCloningKey))
        voiceCloneAnalytics?.trackCreated(job.parentId, jobId, profile.id)
        log.info("Voice cloning completed (Google): jobId={} parentId={}", jobId, job.parentId)
    }

    private fun processWithElevenLabs(jobId: Long, job: VoiceCloningJob) {
        val audioBytes = voiceReferenceStorage.getReferenceAudio(job.audioStoragePath)
            ?: throw IllegalStateException("Audio file not found or empty: ${job.audioStoragePath}")
        if (audioBytes.isEmpty()) throw IllegalStateException("Audio file is empty: ${job.audioStoragePath}")

        val adapter = elevenLabsAdapter
            ?: throw IllegalStateException("ElevenLabs adapter not configured. Set ELEVENLABS_API_KEY.")
        val voiceId = adapter.addVoice(
            audioBytes = audioBytes,
            fileName = "voice_${job.voiceName}.${referenceAudioExtension(job.audioStoragePath)}",
            name = job.voiceName
        ) ?: throw IllegalStateException("ElevenLabs voice creation failed")

        // Avoid false READY: verify the newly created voice can actually synthesize.
        // This catches provider cases where voice ID is returned but no playable sample exists.
        val smokeBytes = adapter.synthesize(
            text = "Hello from Tamixa.",
            voiceId = voiceId,
            language = "en"
        )
        if (smokeBytes == null || smokeBytes.isEmpty()) {
            // Don't leave an unusable voice occupying one of the account's custom voice slots.
            adapter.deleteVoice(voiceId)
            throw IllegalStateException(
                "ElevenLabs voice was created but is not playable (sample missing/invalid). " +
                    "Re-upload a clear 10-30s speech sample (MP3/WAV), then run job again."
            )
        }

        val profiles = voiceRepository.findByParentId(job.parentId)
        val existing = profileOfJob(jobId, job, profiles) ?: run {
            adapter.deleteVoice(voiceId)
            return
        }
        voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.READY, null, voiceId, null, null)
        log.debug("Updating VoiceProfile id={} with ElevenLabs voiceId for parentId={}", existing.id, job.parentId)
        val profile = voiceRepository.save(existing.copy(elevenlabsVoiceId = voiceId))
        // The replaced voice would otherwise stay at ElevenLabs, using a slot nobody can reach.
        existing.elevenlabsVoiceId?.takeIf { it.isNotBlank() && it != voiceId }?.let { old ->
            val others = profiles.filter { it.id != existing.id }
            afterCommit { releaseProviderVoices(existing.copy(fishAudioModelId = null), others) }
            log.info("Replacing ElevenLabs voice {} on profileId={}", old.take(8), existing.id)
        }
        voiceCloneAnalytics?.trackCreated(job.parentId, jobId, profile.id)
        log.info("Voice cloning completed (ElevenLabs): jobId={} voiceId={}", jobId, voiceId.take(8), StructuredArguments.kv("parentId", job.parentId))
    }

    private fun processWithFishAudio(jobId: Long, job: VoiceCloningJob) {
        val audioBytes = voiceReferenceStorage.getReferenceAudio(job.audioStoragePath)
            ?: throw IllegalStateException("Audio file not found or empty: ${job.audioStoragePath}")
        if (audioBytes.isEmpty()) throw IllegalStateException("Audio file is empty: ${job.audioStoragePath}")

        val adapter = fishAudioAdapter
            ?: throw IllegalStateException("Fish Audio adapter not configured. Set FISH_AUDIO_API_KEY.")
        val modelId = adapter.createModelFromSample(
            audioBytes = audioBytes,
            fileName = "voice_${job.voiceName}.${referenceAudioExtension(job.audioStoragePath)}",
            title = job.voiceName.take(200)
        ) ?: throw IllegalStateException("Fish Audio model creation failed")

        val smokeBytes = adapter.synthesize(
            text = "Hello from Tamixa.",
            modelId = modelId,
            language = "en"
        )
        if (smokeBytes == null || smokeBytes.isEmpty()) {
            adapter.deleteModel(modelId)
            throw IllegalStateException(
                "Fish Audio model was created but is not playable. " +
                    "Re-upload a clear 10-30s speech sample (MP3/WAV), then run job again."
            )
        }

        val profiles = voiceRepository.findByParentId(job.parentId)
        val existing = profileOfJob(jobId, job, profiles) ?: run {
            adapter.deleteModel(modelId)
            return
        }
        voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.READY, null, null, null, modelId)
        log.debug("Updating VoiceProfile id={} with Fish Audio modelId for parentId={}", existing.id, job.parentId)
        val profile = voiceRepository.save(existing.copy(fishAudioModelId = modelId))
        existing.fishAudioModelId?.takeIf { it.isNotBlank() && it != modelId }?.let { old ->
            val others = profiles.filter { it.id != existing.id }
            afterCommit { releaseProviderVoices(existing.copy(elevenlabsVoiceId = null), others) }
            log.info("Replacing Fish Audio model {} on profileId={}", old.take(12), existing.id)
        }
        voiceCloneAnalytics?.trackCreated(job.parentId, jobId, profile.id)
        log.info("Voice cloning completed (Fish Audio): jobId={} modelId={}", jobId, modelId.take(12), StructuredArguments.kv("parentId", job.parentId))
    }

    fun getVoiceCloningJobs(parentId: Long): List<VoiceCloningJob> {
        return voiceCloningJobRepository.findByParentId(parentId)
    }

    fun getVoiceCloningJob(jobId: Long): VoiceCloningJob? {
        return voiceCloningJobRepository.findById(jobId)
    }

    fun getReadyVoices(parentId: Long): List<VoiceCloningJob> {
        return voiceCloningJobRepository.findByParentId(parentId)
            .filter { it.status == VoiceCloningStatus.READY }
    }

    /**
     * Create a VoiceProfile backed by a reference audio sample.
     *
     * This path is used for self-hosted voice cloning (XTTS-style) and does not
     * require ElevenLabs. The reference audio is stored via VoiceReferenceStoragePort
     * and the resulting storage path is recorded on the VoiceProfile.
     * Like [uploadVoice], a repeat of an already uploaded recording returns the existing profile.
     */
    fun createReferenceVoiceProfile(parentEmail: String, audioBytes: ByteArray, fileName: String): VoiceProfile {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        return synchronized(uploadLock(parent.id)) {
            findDuplicateUpload(parent.id, fileName, audioBytes)?.let { existing ->
                log.info("Reference voice upload is a repeat of profileId={} parentId={}; returning it", existing.id, parent.id)
                return@synchronized existing
            }
            checkMaxClonedVoices(parent.id)

            val storagePath = voiceReferenceStorage.storeReferenceAudio(parent.id, resolveReferenceFileName(fileName, null), audioBytes)
            val profile = voiceRepository.save(
                VoiceProfile(
                    id = 0,
                    parentId = parent.id,
                    encryptedEmbedding = ByteArray(0),
                    createdAt = Instant.now(),
                    elevenlabsVoiceId = null,
                    fishAudioModelId = null,
                    googleVoiceCloningKey = null,
                    referenceAudioPath = storagePath,
                    heygenVoiceId = null
                )
            )
            voiceCloneAnalytics?.trackCreated(parent.id, null, profile.id)
            profile
        }
    }

    /**
     * Create a Google voice cloning job from an existing voice profile (reference already uploaded)
     * by uploading consent audio. For use when VOICE_CLONING_PROVIDER=google and the profile has
     * reference_audio_path but no google_voice_cloning_key (e.g. admin uploaded reference only).
     * Stores consent in S3, creates job, and runs it asynchronously; the job will attach the
     * resulting key to the profile whose reference path matches.
     *
     * @return The created job (status PENDING then PROCESSING). Poll job status or retry preview after completion.
     * @throws IllegalArgumentException if profile not found, has no reference path, or Google provider not configured.
     */
    @Transactional
    fun createGoogleVoiceCloningJobFromProfile(
        parentId: Long,
        profileId: Long,
        consentBytes: ByteArray,
        consentFileName: String
    ): VoiceCloningJob {
        val profile = voiceRepository.findByIdAndParentId(profileId, parentId)
            ?: throw IllegalArgumentException("Voice profile not found: profileId=$profileId parentId=$parentId")
        val referencePath = profile.referenceAudioPath
            ?: throw IllegalArgumentException("Voice profile has no reference audio. Upload reference first via POST /parents/{parentId}/voice/upload")
        if (consentBytes.isEmpty()) throw IllegalArgumentException("Consent audio must not be empty")
        val useGoogle = voiceCloningConfig.provider == "google" && googleCloudAdapter != null
        if (!useGoogle || !voiceCloningConfig.enabled)
            throw IllegalArgumentException("Google voice cloning not configured. Set VOICE_CLONING_ENABLED=true, VOICE_CLONING_PROVIDER=google, and GOOGLE_CLOUD_TTS_API_KEY in backend .env")
        val ts = System.currentTimeMillis()
        val consentPath = voiceReferenceStorage.storeReferenceAudio(parentId, "cloning_${ts}_consent_$consentFileName", consentBytes)
        val job = createVoiceCloningJob(
            parentId = parentId,
            audioStoragePath = referencePath,
            consentAudioStoragePath = consentPath,
            audioFileSizeBytes = 0L,
            voiceName = deriveVoiceNameFromReferencePath(referencePath, profileId)
        )
        processVoiceCloningJob(job.id)
        log.info("Admin Google voice cloning job created and started: jobId={} parentId={} profileId={}", job.id, parentId, profileId)
        return job
    }

    /**
     * Create a no-consent managed cloud clone job (ElevenLabs or Fish Audio) from an existing voice profile.
     * For use when the profile has reference_audio_path but no cloud clone id yet (e.g. admin uploaded reference only).
     *
     * @return The created job (status PENDING then PROCESSING). Retry preview after completion.
     * @throws IllegalArgumentException if profile not found, has no reference path, or no managed provider is configured.
     */
    @Transactional
    fun createElevenLabsVoiceCloningJobFromProfile(parentId: Long, profileId: Long): VoiceCloningJob {
        val profile = voiceRepository.findByIdAndParentId(profileId, parentId)
            ?: throw IllegalArgumentException("Voice profile not found: profileId=$profileId parentId=$parentId")
        val referencePath = profile.referenceAudioPath
            ?: throw IllegalArgumentException("Voice profile has no reference audio. Upload reference first via POST /parents/{parentId}/voice/upload")
        val provider = voiceCloningConfig.provider.trim().lowercase()
        val useElevenLabsPrimary =
            provider == "elevenlabs" &&
                voiceCloningConfig.elevenLabsApiKey.isNotBlank() &&
                elevenLabsAdapter != null
        val useElevenLabsFallbackFromGoogle =
            provider == "google" &&
                voiceCloningConfig.allowElevenLabsFallback &&
                voiceCloningConfig.elevenLabsApiKey.isNotBlank() &&
                elevenLabsAdapter != null
        val useFishPrimary =
            provider == "fishaudio" &&
                voiceCloningConfig.fishAudioApiKey.isNotBlank() &&
                fishAudioAdapter != null
        val useFishFallbackFromGoogle =
            provider == "google" &&
                voiceCloningConfig.allowFishAudioFallback &&
                voiceCloningConfig.fishAudioApiKey.isNotBlank() &&
                fishAudioAdapter != null
        val useManagedCloud =
            useElevenLabsPrimary || useElevenLabsFallbackFromGoogle || useFishPrimary || useFishFallbackFromGoogle
        if (!useManagedCloud || !voiceCloningConfig.enabled) {
            throw IllegalArgumentException(
                "Managed cloud voice cloning not configured. Set VOICE_CLONING_ENABLED=true and either " +
                    "ELEVENLABS_API_KEY (VOICE_CLONING_PROVIDER=elevenlabs or VOICE_CLONING_ALLOW_ELEVENLABS_FALLBACK=true with Google) " +
                    "or FISH_AUDIO_API_KEY (VOICE_CLONING_PROVIDER=fishaudio or VOICE_CLONING_ALLOW_FISH_AUDIO_FALLBACK=true with Google)."
            )
        }
        val job = createVoiceCloningJob(
            parentId = parentId,
            audioStoragePath = referencePath,
            consentAudioStoragePath = null,
            audioFileSizeBytes = 0L,
            voiceName = deriveVoiceNameFromReferencePath(referencePath, profileId)
        )
        processVoiceCloningJob(job.id)
        log.info("Admin managed cloud voice cloning job created and started: jobId={} parentId={} profileId={}", job.id, parentId, profileId)
        return job
    }

    /**
     * Create a VoiceProfile for a parent by ID (admin/testing).
     * Same as createReferenceVoiceProfile but looks up parent by ID.
     */
    @Transactional
    fun createReferenceVoiceProfileByParentId(
        parentId: Long,
        audioBytes: ByteArray,
        fileName: String,
        profileName: String? = null
    ): VoiceProfile {
        val parent = parentRepository.findById(parentId)
            ?: throw IllegalArgumentException("Parent not found: $parentId")
        val targetName = normalizeProfileName(profileName?.takeIf { it.isNotBlank() } ?: fileName.substringBeforeLast('.'))
        val existingByName = if (targetName.isNotBlank()) {
            voiceRepository.findByParentId(parent.id)
                .firstOrNull { profile ->
                    normalizeProfileName(extractProfileNameFromPath(profile.referenceAudioPath)) == targetName
                }
        } else {
            null
        }

        val storagePath = voiceReferenceStorage.storeReferenceAudio(
            parent.id,
            resolveReferenceFileName(fileName, profileName),
            audioBytes
        )

        if (existingByName != null) {
            val oldPath = existingByName.referenceAudioPath
            val updated = voiceRepository.save(
                existingByName.copy(
                    referenceAudioPath = storagePath,
                    elevenlabsVoiceId = null,
                    fishAudioModelId = null,
                    googleVoiceCloningKey = null
                )
            )
            if (!oldPath.isNullOrBlank() && oldPath != storagePath) {
                voiceReferenceStorage.deleteReferenceAudio(oldPath)
            }
            val others = voiceRepository.findByParentId(parent.id).filter { it.id != existingByName.id }
            afterCommit { releaseProviderVoices(existingByName, others) }
            log.info(
                "Updated existing voice profile by name: parentId={} profileId={} profileName={}",
                parent.id,
                updated.id,
                profileName?.take(64) ?: extractProfileNameFromPath(storagePath)
            )
            return updated
        }

        checkMaxClonedVoices(parent.id)
        val profile = voiceRepository.save(
            VoiceProfile(
                id = 0,
                parentId = parent.id,
                encryptedEmbedding = ByteArray(0),
                createdAt = Instant.now(),
                elevenlabsVoiceId = null,
                fishAudioModelId = null,
                googleVoiceCloningKey = null,
                referenceAudioPath = storagePath,
                heygenVoiceId = null
            )
        )
        voiceCloneAnalytics?.trackCreated(parent.id, null, profile.id)
        return profile
    }

    private fun resolveReferenceFileName(fileName: String, profileName: String?): String {
        val cleanedName = profileName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.replace(Regex("[^A-Za-z0-9_-]+"), "_")
            ?.replace(Regex("_+"), "_")
            ?.trim('_')
            ?.take(64)
            ?.ifBlank { null }

        val normalizedFileName = fileName.trim().ifBlank { "voice.mp3" }
        val ext = normalizedFileName.substringAfterLast('.', "").trim().lowercase()
            .takeIf { it.matches(Regex("[a-z0-9]{1,6}")) }
            ?: "mp3"
        return if (cleanedName != null) "$cleanedName.$ext" else normalizedFileName
    }

    private fun deriveVoiceNameFromReferencePath(referencePath: String, profileId: Long): String {
        val baseName = referencePath.substringAfterLast('/').substringBeforeLast('.').trim()
        val cleaned = baseName
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(64)
        return if (cleaned.isNotBlank()) cleaned else "voice_$profileId"
    }

    private fun extractProfileNameFromPath(referencePath: String?): String {
        if (referencePath.isNullOrBlank()) return ""
        return referencePath.substringAfterLast('/').substringBeforeLast('.').trim()
            .replace(Regex("^cloning_\\d+_ref_"), "")
    }

    private fun normalizeProfileName(name: String): String {
        return name.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Admin: set HeyGen voice_id on a voice profile so cloned narration can use HeyGen TTS.
     * Create the voice in HeyGen app first, then pass the voice_id here.
     */
    @Transactional
    fun updateHeyGenVoiceId(parentId: Long, profileId: Long, heygenVoiceId: String?): VoiceProfile? {
        val profile = voiceRepository.findByIdAndParentId(profileId, parentId) ?: return null
        val updated = profile.copy(heygenVoiceId = heygenVoiceId?.takeIf { it.isNotBlank() })
        return voiceRepository.save(updated)
    }

    /**
     * Admin: delete a reference voice profile for a parent so a new sample can be uploaded.
     * Removes S3 reference audio when present, then deletes the DB row.
     */
    @Transactional
    fun deleteReferenceVoiceProfileByParentId(parentId: Long, profileId: Long) {
        val profile = voiceRepository.findByIdAndParentId(profileId, parentId)
            ?: throw IllegalArgumentException("Voice profile not found for parent: profileId=$profileId parentId=$parentId")
        val path = profile.referenceAudioPath
        val others = voiceRepository.findByParentId(parentId).filter { it.id != profileId }
        // Keep the stored file while another of the parent's profiles still points at it (e.g. a re-uploaded recording).
        val sharedByOther = !path.isNullOrBlank() && others.any { it.referenceAudioPath == path }
        voiceRepository.deleteById(profileId)
        // Its cloning jobs go with it: they hold the recording's path, provider voice id and name.
        val jobs = if (path.isNullOrBlank() || sharedByOther) emptyList()
        else voiceCloningJobRepository.findByParentId(parentId).filter { it.audioStoragePath == path }
        voiceCloningJobRepository.deleteAllById(jobs.map { it.id })
        val consentPaths = jobs.mapNotNull { it.consentAudioStoragePath?.takeIf(String::isNotBlank) }.distinct()
        // Story narrations in this voice go with it (rows now, in this transaction; files after commit).
        val narrationFiles = clonedVoiceMedia?.detachMedia(listOf(profileId)).orEmpty()
        // Remove the files and the provider voice only after the row is gone: if the delete is refused (e.g. a story
        // still uses this voice) the transaction rolls back and all of it must still exist.
        afterCommit {
            if (!path.isNullOrBlank() && !sharedByOther) voiceReferenceStorage.deleteReferenceAudio(path)
            consentPaths.forEach { voiceReferenceStorage.deleteReferenceAudio(it) }
            releaseProviderVoices(profile, others)
            clonedVoiceMedia?.deleteFiles(narrationFiles)
            // Narration files without a database row (e.g. generated stories) are found by the sweep.
            clonedVoiceMedia?.reconcileOrphansAsync()
        }
        log.info("Deleted voice profile id={} parentId={} cloningJobs={}", profileId, parentId, jobs.size)
    }

    /**
     * Everything of the parent's voices that lives outside the database: stored recordings (reference and consent
     * audio, from profiles and cloning jobs), cloned voices at ElevenLabs / Fish Audio, and story narrations made
     * with those voices. Collect it before the account rows are deleted, then pass it to [purgeVoiceData].
     */
    fun collectVoiceData(parentId: Long): ParentVoiceData {
        val profiles = voiceRepository.findByParentId(parentId)
        val jobs = voiceCloningJobRepository.findByParentId(parentId)
        return ParentVoiceData(
            profileIds = profiles.map { it.id }.toSet(),
            storagePaths = (profiles.map { it.referenceAudioPath } + jobs.flatMap { listOf(it.audioStoragePath, it.consentAudioStoragePath) })
                .filterNotNull().filter { it.isNotBlank() }.toSet(),
            elevenLabsVoiceIds = (profiles.map { it.elevenlabsVoiceId } + jobs.map { it.elevenLabsVoiceId })
                .filterNotNull().filter { it.isNotBlank() }.toSet(),
            fishAudioModelIds = (profiles.map { it.fishAudioModelId } + jobs.map { it.fishAudioModelId })
                .filterNotNull().filter { it.isNotBlank() }.toSet()
        )
    }

    /**
     * Account deletion: removes the parent's stored recordings and provider voices (see [collectVoiceData]).
     * Best effort per item; anything that could not be removed is logged at ERROR with its id for manual cleanup.
     * Google voice cloning keys are not stored resources at Google and need no deletion.
     */
    fun purgeVoiceData(parentId: Long, data: ParentVoiceData) {
        var failed = 0
        data.storagePaths.forEach { path ->
            runCatching { voiceReferenceStorage.deleteReferenceAudio(path) }
                .onFailure { failed++; log.error("Account deletion: removing recording failed parentId={} path={}", parentId, path, it) }
        }
        data.elevenLabsVoiceIds.forEach { voiceId ->
            if (elevenLabsAdapter?.deleteVoice(voiceId) != true) {
                failed++
                log.error("Account deletion: ElevenLabs voice not deleted parentId={} voiceId={}", parentId, voiceId)
            }
        }
        data.fishAudioModelIds.forEach { modelId ->
            if (fishAudioAdapter?.deleteModel(modelId) != true) {
                failed++
                log.error("Account deletion: Fish Audio model not deleted parentId={} modelId={}", parentId, modelId)
            }
        }
        clonedVoiceMedia?.let { media ->
            failed += runCatching { media.deleteFiles(media.detachMedia(data.profileIds)) }
                .onFailure { log.error("Account deletion: removing cloned-voice narrations failed parentId={}", parentId, it) }
                .getOrDefault(1)
            media.reconcileOrphansAsync()
        }
        log.info(
            "Account deletion: voice data purged parentId={} recordings={} elevenLabsVoices={} fishModels={} failed={}",
            parentId, data.storagePaths.size, data.elevenLabsVoiceIds.size, data.fishAudioModelIds.size, failed
        )
    }

    /** Runs [action] once the current transaction commits, or right away when there is none. */
    private fun afterCommit(action: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = action()
            })
        } else {
            action()
        }
    }

    /**
     * Deletes [profile]'s cloned voice at ElevenLabs / Fish Audio unless one of [others] still uses the same id.
     * Provider accounts have a small custom-voice limit, so voices of deleted profiles must not linger there.
     * Failures are logged by the adapters and never fail the caller.
     */
    private fun releaseProviderVoices(profile: VoiceProfile, others: List<VoiceProfile>) {
        val voiceId = profile.elevenlabsVoiceId
        if (!voiceId.isNullOrBlank() && others.none { it.elevenlabsVoiceId == voiceId }) {
            elevenLabsAdapter?.deleteVoice(voiceId)
                ?: log.warn("ElevenLabs voice of profileId={} not deleted: ElevenLabs not configured", profile.id)
        }
        val modelId = profile.fishAudioModelId
        if (!modelId.isNullOrBlank() && others.none { it.fishAudioModelId == modelId }) {
            fishAudioAdapter?.deleteModel(modelId)
                ?: log.warn("Fish Audio model of profileId={} not deleted: Fish Audio not configured", profile.id)
        }
    }

    /**
     * Admin: return reference audio bytes for a voice profile (for preview in admin UI).
     * Returns null if profile not found or has no reference audio.
     */
    fun getReferenceAudioForAdmin(parentId: Long, profileId: Long): ByteArray? {
        val profile = voiceRepository.findByIdAndParentId(profileId, parentId) ?: return null
        val path = profile.referenceAudioPath ?: return null
        return voiceReferenceStorage.getReferenceAudio(path)
    }

    /**
     * Upload voice sample(s): stores the reference in S3, creates the voice profile right away and a cloning job
     * that later attaches the cloud voice to that profile (see processWith*: they update the profile with the same
     * reference path). Callers start the job with [processVoiceCloningJob] when [VoiceUploadResult.job] is not null.
     * For Google: pass both referenceBytes and consentBytes. For ElevenLabs: pass referenceBytes only.
     *
     * Idempotent per recording: re-sending a file this parent already uploaded (same file name and same bytes,
     * e.g. a client retry after a timeout) returns the existing profile and creates nothing, even at the voice limit.
     * Uploads are serialized per parent so a retry that overlaps a still-running upload sees its result.
     * The lock is in-process; with several backend instances a concurrent retry can still create a duplicate.
     */
    fun uploadVoice(
        parentEmail: String,
        referenceBytes: ByteArray,
        fileName: String,
        consentBytes: ByteArray? = null
    ): VoiceUploadResult {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        return synchronized(uploadLock(parent.id)) {
            findDuplicateUpload(parent.id, fileName, referenceBytes)?.let { existing ->
                log.info("Voice upload is a repeat of profileId={} parentId={}; returning it", existing.id, parent.id)
                return@synchronized VoiceUploadResult(profile = existing, job = null)
            }
            checkMaxClonedVoices(parent.id)

            val ts = System.currentTimeMillis()
            val storagePath = voiceReferenceStorage.storeReferenceAudio(parent.id, "cloning_${ts}_ref_$fileName", referenceBytes)
            val consentPath = consentBytes?.let { bytes ->
                if (bytes.isNotEmpty()) voiceReferenceStorage.storeReferenceAudio(parent.id, "cloning_${ts}_consent_$fileName", bytes)
                else null
            }
            // Profile and job commit together, before the lock is released and before the async job can look for the profile.
            inTransaction {
                val profile = voiceRepository.save(
                    VoiceProfile(
                        id = 0,
                        parentId = parent.id,
                        encryptedEmbedding = ByteArray(0),
                        createdAt = Instant.now(),
                        referenceAudioPath = storagePath
                    )
                )
                val job = voiceCloningJobRepository.save(
                    VoiceCloningJob(
                        id = 0,
                        parentId = parent.id,
                        audioStoragePath = storagePath,
                        consentAudioStoragePath = consentPath,
                        audioFileSizeBytes = referenceBytes.size.toLong(),
                        voiceName = fileName.substringBeforeLast('.').takeIf { it.isNotBlank() } ?: "voice",
                        elevenLabsVoiceId = null,
                        fishAudioModelId = null,
                        googleVoiceCloningKey = null,
                        status = VoiceCloningStatus.PENDING,
                        errorMessage = null,
                        createdAt = Instant.now(),
                        completedAt = null
                    )
                )
                log.info("Voice uploaded: profileId={} jobId={} parentId={}", profile.id, job.id, parent.id)
                VoiceUploadResult(profile = profile, job = job)
            }
        }
    }

    private val uploadLocks = java.util.concurrent.ConcurrentHashMap<Long, Any>()

    private fun uploadLock(parentId: Long): Any = uploadLocks.computeIfAbsent(parentId) { Any() }

    private fun <T> inTransaction(block: () -> T): T =
        transactionTemplate?.execute { block() } ?: block()

    /**
     * The parent's existing profile for this exact recording: same original file name and same bytes.
     * When the stored file can't be read back, the size recorded on its cloning job is compared instead.
     */
    private fun findDuplicateUpload(parentId: Long, fileName: String, bytes: ByteArray): VoiceProfile? {
        val sameName = voiceRepository.findByParentId(parentId)
            .filter { originalReferenceFileName(it.referenceAudioPath) == fileName }
        if (sameName.isEmpty()) return null
        val jobs by lazy { voiceCloningJobRepository.findByParentId(parentId) }
        return sameName.firstOrNull { profile ->
            val path = profile.referenceAudioPath!!
            val stored = runCatching { voiceReferenceStorage.getReferenceAudio(path) }
                .onFailure { log.warn("Reading stored reference for duplicate check failed profileId={}", profile.id, it) }
                .getOrNull()
            if (stored != null) stored.contentEquals(bytes)
            else jobs.any { it.audioStoragePath == path && it.audioFileSizeBytes == bytes.size.toLong() }
        }
    }
}

/** A parent's voice data outside the database; see [VoiceCloningService.collectVoiceData]. */
data class ParentVoiceData(
    val storagePaths: Set<String>,
    val elevenLabsVoiceIds: Set<String>,
    val fishAudioModelIds: Set<String>,
    /** Voice profile ids, for removing story narrations made with them. */
    val profileIds: Set<Long> = emptySet()
)

/** Result of [VoiceCloningService.uploadVoice]: the persistent profile, and the job to start (null for a repeat upload). */
data class VoiceUploadResult(val profile: VoiceProfile, val job: VoiceCloningJob?)

/** Extension of a stored reference recording (e.g. "m4a"), so providers get the real audio type; "mp3" when unknown. */
fun referenceAudioExtension(referenceAudioPath: String?): String =
    referenceAudioPath?.substringAfterLast('/')?.substringAfterLast('.', "")?.lowercase()
        ?.takeIf { it.matches(Regex("[a-z0-9]{1,6}")) }
        ?: "mp3"

/** File name the client uploaded, without the "cloning_{ts}_ref_" storage prefix (e.g. "my_voice_1791546447445.m4a"). */
fun originalReferenceFileName(referenceAudioPath: String?): String? =
    referenceAudioPath?.substringAfterLast('/')
        ?.replace(Regex("^cloning_\\d+_ref_"), "")
        ?.takeIf { it.isNotBlank() }

class VoiceCloningJobNotFoundException(id: Long) : RuntimeException("Voice cloning job not found: $id")

class MaxClonedVoicesReachedException(message: String) : IllegalArgumentException(message)

/** The recording already has a cloud clone, or one is in progress (409). */
class VoiceAlreadyClonedException(message: String) : RuntimeException(message)
