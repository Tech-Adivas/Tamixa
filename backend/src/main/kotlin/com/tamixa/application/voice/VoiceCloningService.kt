package com.tamixa.application.voice

import com.tamixa.application.analytics.VoiceCloneAnalyticsService
import com.tamixa.application.port.VoiceCloningJobRepositoryPort
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.port.VoiceRepositoryPort
import com.tamixa.application.port.voice.ElevenLabsVoiceCloningPort
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
import java.time.Instant

@Service
class VoiceCloningService(
    private val voiceCloningJobRepository: VoiceCloningJobRepositoryPort,
    private val parentRepository: ParentRepositoryPort,
    private val voiceRepository: VoiceRepositoryPort,
    private val voiceReferenceStorage: VoiceReferenceStoragePort,
    @Autowired(required = false) private val elevenLabsAdapter: ElevenLabsVoiceCloningPort?,
    @Autowired(required = false) private val googleCloudAdapter: GoogleCloudVoiceCloningPort?,
    private val appProperties: AppProperties,
    @Autowired(required = false) private val voiceCloneAnalytics: VoiceCloneAnalyticsService?
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
            googleVoiceCloningKey = null,
            status = VoiceCloningStatus.PENDING,
            errorMessage = null,
            createdAt = Instant.now(),
            completedAt = null
        )
        return voiceCloningJobRepository.save(job)
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
        val canFallbackToElevenLabsFromGoogle = useGooglePrimary &&
            voiceCloningConfig.allowElevenLabsFallback &&
            voiceCloningConfig.elevenLabsApiKey.isNotBlank() &&
            elevenLabsAdapter != null

        if (!voiceCloningConfig.enabled || (!useGooglePrimary && !useElevenLabsPrimary)) {
            val msg = "Set VOICE_CLONING_ENABLED=true and VOICE_CLONING_PROVIDER=google (with GOOGLE_CLOUD_TTS_API_KEY) or provider=elevenlabs (with ELEVENLABS_API_KEY)"
            log.error(
                "Voice cloning job cannot be processed: jobId={} provider={}",
                jobId,
                voiceCloningConfig.provider,
                StructuredArguments.kv("parentId", job.parentId)
            )
            voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.FAILED, msg, null, null)
            return
        }

        voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.PROCESSING, null, null, null)

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
                    } else {
                        try {
                            processWithGoogle(jobId, job)
                        } catch (googleError: Exception) {
                            if (!canFallbackToElevenLabsFromGoogle) throw googleError
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
                                throw IllegalStateException(
                                    "Google clone failed: ${googleError.message ?: "unknown"}; ElevenLabs fallback failed: ${fallbackError.message ?: "unknown"}",
                                    fallbackError
                                )
                            }
                        }
                    }
                }
                useElevenLabsPrimary -> processWithElevenLabs(jobId, job)
                else -> throw IllegalStateException("No voice cloning provider")
            }
        } catch (e: Exception) {
            log.error("Voice cloning failed: jobId={}", jobId, e)
            voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.FAILED, e.message ?: "Unknown error", null, null)
        }
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

        voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.READY, null, null, voiceCloningKey)

        // Update the profile that provided the reference (same path as job), or first with key, or first.
        val profiles = voiceRepository.findByParentId(job.parentId)
        val existing = profiles.firstOrNull { it.referenceAudioPath == job.audioStoragePath }
            ?: profiles.firstOrNull { it.googleVoiceCloningKey != null }
            ?: profiles.firstOrNull()
        val profile = if (existing != null) {
            log.debug("Updating VoiceProfile id={} with Google key for parentId={}", existing.id, job.parentId)
            voiceRepository.save(existing.copy(googleVoiceCloningKey = voiceCloningKey))
        } else {
            voiceRepository.save(
                VoiceProfile(
                    id = 0,
                    parentId = job.parentId,
                    encryptedEmbedding = ByteArray(0),
                    createdAt = Instant.now(),
                    elevenlabsVoiceId = null,
                    googleVoiceCloningKey = voiceCloningKey,
                    referenceAudioPath = null,
                    heygenVoiceId = null
                )
            )
        }
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
            fileName = "voice_${job.voiceName}.mp3",
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
            throw IllegalStateException(
                "ElevenLabs voice was created but is not playable (sample missing/invalid). " +
                    "Re-upload a clear 10-30s speech sample (MP3/WAV), then run job again."
            )
        }

        voiceCloningJobRepository.updateStatus(jobId, VoiceCloningStatus.READY, null, voiceId, null)

        // Prefer the profile that provided the reference (same path as job), so admin-run job attaches to that profile.
        val profiles = voiceRepository.findByParentId(job.parentId)
        val existingByRef = profiles.firstOrNull { it.referenceAudioPath == job.audioStoragePath }
        val existing = existingByRef ?: profiles.firstOrNull { it.elevenlabsVoiceId == voiceId }
        val profile = if (existing != null) {
            val toSave = if (existingByRef != null) existing.copy(elevenlabsVoiceId = voiceId) else existing
            if (toSave != existing) {
                log.debug("Updating VoiceProfile id={} with ElevenLabs voiceId for parentId={}", existing.id, job.parentId)
                voiceRepository.save(toSave)
            } else existing
        } else {
            voiceRepository.save(
                VoiceProfile(
                    id = 0,
                    parentId = job.parentId,
                    encryptedEmbedding = ByteArray(0),
                    createdAt = Instant.now(),
                    elevenlabsVoiceId = voiceId,
                    googleVoiceCloningKey = null,
                    referenceAudioPath = null,
                    heygenVoiceId = null
                )
            )
        }
        voiceCloneAnalytics?.trackCreated(job.parentId, jobId, profile.id)
        log.info("Voice cloning completed (ElevenLabs): jobId={} voiceId={}", jobId, voiceId.take(8), StructuredArguments.kv("parentId", job.parentId))
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
     */
    @Transactional
    fun createReferenceVoiceProfile(parentEmail: String, audioBytes: ByteArray, fileName: String): VoiceProfile {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        checkMaxClonedVoices(parent.id)

        val storagePath = voiceReferenceStorage.storeReferenceAudio(parent.id, resolveReferenceFileName(fileName, null), audioBytes)
        val profile = voiceRepository.save(
            VoiceProfile(
                id = 0,
                parentId = parent.id,
                encryptedEmbedding = ByteArray(0),
                createdAt = Instant.now(),
                elevenlabsVoiceId = null,
                googleVoiceCloningKey = null,
                referenceAudioPath = storagePath,
                heygenVoiceId = null
            )
        )
        voiceCloneAnalytics?.trackCreated(parent.id, null, profile.id)
        return profile
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
     * Create an ElevenLabs voice cloning job from an existing voice profile (reference already uploaded).
     * No consent required. For use when VOICE_CLONING_PROVIDER=elevenlabs and the profile has
     * reference_audio_path but no elevenlabs_voice_id (e.g. admin uploaded reference only).
     *
     * @return The created job (status PENDING then PROCESSING). Retry preview after completion.
     * @throws IllegalArgumentException if profile not found, has no reference path, or ElevenLabs not configured.
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
        val useElevenLabs = useElevenLabsPrimary || useElevenLabsFallbackFromGoogle
        if (!useElevenLabs || !voiceCloningConfig.enabled)
            throw IllegalArgumentException(
                "ElevenLabs voice cloning not configured. Set VOICE_CLONING_ENABLED=true and ELEVENLABS_API_KEY, " +
                    "with VOICE_CLONING_PROVIDER=elevenlabs, or keep provider=google with VOICE_CLONING_ALLOW_ELEVENLABS_FALLBACK=true."
            )
        val job = createVoiceCloningJob(
            parentId = parentId,
            audioStoragePath = referencePath,
            consentAudioStoragePath = null,
            audioFileSizeBytes = 0L,
            voiceName = deriveVoiceNameFromReferencePath(referencePath, profileId)
        )
        processVoiceCloningJob(job.id)
        log.info("Admin ElevenLabs voice cloning job created and started: jobId={} parentId={} profileId={}", job.id, parentId, profileId)
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
                    googleVoiceCloningKey = null
                )
            )
            if (!oldPath.isNullOrBlank() && oldPath != storagePath) {
                voiceReferenceStorage.deleteReferenceAudio(oldPath)
            }
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
        if (!profile.referenceAudioPath.isNullOrBlank()) {
            voiceReferenceStorage.deleteReferenceAudio(profile.referenceAudioPath)
        }
        voiceRepository.deleteById(profileId)
        log.info("Deleted voice profile id={} parentId={}", profileId, parentId)
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
     * Upload voice sample and create ElevenLabs voice cloning job.
     * Stores audio in S3 via VoiceReferenceStoragePort, then creates job and triggers async processing.
     */
    /**
     * Upload voice sample(s) and create cloning job.
     * For Google: pass both referenceBytes and consentBytes.
     * For ElevenLabs: pass referenceBytes only (consentBytes ignored).
     */
    @Transactional
    fun uploadVoice(
        parentEmail: String,
        referenceBytes: ByteArray,
        fileName: String,
        consentBytes: ByteArray? = null
    ): VoiceCloningJob {
        val parent = parentRepository.findByEmail(parentEmail)
            ?: throw com.tamixa.application.auth.ParentNotFoundException(parentEmail)
        checkMaxClonedVoices(parent.id)

        val ts = System.currentTimeMillis()
        val storagePath = voiceReferenceStorage.storeReferenceAudio(parent.id, "cloning_${ts}_ref_$fileName", referenceBytes)
        val consentPath = consentBytes?.let { bytes ->
            if (bytes.isNotEmpty()) voiceReferenceStorage.storeReferenceAudio(parent.id, "cloning_${ts}_consent_$fileName", bytes)
            else null
        }

        val job = VoiceCloningJob(
            id = 0,
            parentId = parent.id,
            audioStoragePath = storagePath,
            consentAudioStoragePath = consentPath,
            audioFileSizeBytes = referenceBytes.size.toLong(),
            voiceName = fileName.substringBeforeLast('.').takeIf { it.isNotBlank() } ?: "voice",
            elevenLabsVoiceId = null,
            googleVoiceCloningKey = null,
            status = VoiceCloningStatus.PENDING,
            errorMessage = null,
            createdAt = Instant.now(),
            completedAt = null
        )
        return voiceCloningJobRepository.save(job)
    }
}

class VoiceCloningJobNotFoundException(id: Long) : RuntimeException("Voice cloning job not found: $id")

class MaxClonedVoicesReachedException(message: String) : IllegalArgumentException(message)
