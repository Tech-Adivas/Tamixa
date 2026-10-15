package com.tamixa.api.voice

import com.tamixa.api.ApiVersion
import com.tamixa.application.consent.ConsentService
import com.tamixa.application.narration.NarrationPremiumVoiceValidator
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.voice.AudioFileSignature
import com.tamixa.application.voice.VoiceCloningService
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Voice cloning: parent uploads audio sample to create cloned voice for story narration.
 * POST /voice/upload - store a voice sample and return its voice profile (cloning runs in the background when a
 *   provider is configured and the plan includes voice cloning); re-uploading the same recording returns the same
 *   profile; 409 at the voice limit; 400 VOICE_CONSENT_REQUIRED when the parent never consented to voice cloning;
 *   400 UNSUPPORTED_AUDIO when the file isn't a recognized audio format (checked from its bytes)
 * GET /voice - list parent's voice profiles
 * GET /voice/{id} - get profile by id
 * GET /voice/{id}/reference-audio - stream the caller's own stored reference recording (playback after save)
 * DELETE /voice/{id} - delete the caller's own voice profile and its stored recording
 */
@RestController
@RequestMapping("${ApiVersion.V1}/voice")
@PreAuthorize("hasRole('PARENT')")
class VoiceController(
    private val voiceCloningService: VoiceCloningService,
    private val voiceRepository: com.tamixa.application.port.VoiceRepositoryPort,
    private val parentRepository: ParentRepositoryPort,
    private val appProperties: AppProperties,
    private val consentService: ConsentService,
    private val premiumVoiceValidator: NarrationPremiumVoiceValidator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadVoice(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(value = "consentFile", required = false) consentFile: MultipartFile?,
        @RequestParam(value = "userConsent", required = false) userConsent: Boolean?
    ): ResponseEntity<Any> {
        if (file.isEmpty) return ResponseEntity.badRequest().build()
        val email = currentParentEmail()
        val parentId = resolveParentId() ?: throw com.tamixa.application.auth.ParentNotFoundException(email)
        val fileName = file.originalFilename ?: "voice.mp3"
        val consentBytes = consentFile?.takeIf { !it.isEmpty }?.bytes
        val referenceBytes = file.bytes
        if (!AudioFileSignature.isSupportedAudio(referenceBytes) ||
            (consentBytes != null && !AudioFileSignature.isSupportedAudio(consentBytes))
        ) {
            log.info("Voice upload refused: not a supported audio file parentId={} bytes={}", parentId, referenceBytes.size)
            return ResponseEntity.badRequest().body(
                mapOf("message" to "Please upload an audio recording (MP3, M4A, WAV, OGG or AAC)", "code" to "UNSUPPORTED_AUDIO")
            )
        }

        // A voice is only stored with the parent's consent: given with this upload, or recorded earlier
        // (POST /consent, used by the app before it records; background sync of saved recordings relies on it).
        if (userConsent == true) {
            consentService.record(email, VOICE_CLONING_CONSENT, 1)
        } else if (!consentService.hasConsent(email, VOICE_CLONING_CONSENT)) {
            log.info("Voice upload refused: no voice cloning consent parentId={}", parentId)
            return ResponseEntity.badRequest().body(
                mapOf("message" to "Please agree to voice cloning before uploading your voice", "code" to "VOICE_CONSENT_REQUIRED")
            )
        }

        // Paid cloud clones are only created for plans that include voice cloning; otherwise the recording is just
        // stored, and the clone is made on first cloned narration after an upgrade (see ClonedVoiceStrategy).
        val voiceCloningConfig = appProperties.voiceCloning
        val provider = voiceCloningConfig.provider.trim().lowercase()
        val canClone = voiceCloningConfig.enabled && premiumVoiceValidator.canCloneVoice(parentId)
        if (voiceCloningConfig.enabled && !canClone) {
            log.info("Voice upload stored without cloning: plan has no voice cloning parentId={}", parentId)
        }
        val useGoogle = canClone && provider == "google"
        val useManagedCloud = canClone && (
            (provider == "elevenlabs" && voiceCloningConfig.elevenLabsApiKey.isNotBlank()) ||
                (provider == "fishaudio" && voiceCloningConfig.fishAudioApiKey.isNotBlank())
            )

        // Every branch responds with the persistent voice profile (its id is what GET/DELETE /voice/{id} take).
        return when {
            useGoogle -> {
                if (consentBytes == null || consentBytes.isEmpty()) {
                    return ResponseEntity.badRequest().build()
                } else {
                    val upload = voiceCloningService.uploadVoice(email, referenceBytes, fileName, consentBytes)
                    upload.job?.let { voiceCloningService.processVoiceCloningJob(it.id) }
                    ResponseEntity.ok(upload.profile.toResponse())
                }
            }
            useManagedCloud -> {
                val upload = voiceCloningService.uploadVoice(email, referenceBytes, fileName, null)
                // A repeat upload returns the existing profile without a job: nothing new to clone.
                upload.job?.let { voiceCloningService.processVoiceCloningJob(it.id) }
                ResponseEntity.ok(upload.profile.toResponse())
            }
            else -> {
                val profile = voiceCloningService.createReferenceVoiceProfile(email, referenceBytes, fileName)
                ResponseEntity.ok(profile.toResponse())
            }
        }
    }

    @GetMapping
    fun listVoices(): ResponseEntity<List<com.tamixa.api.voice.dto.VoiceProfileResponse>> {
        val parentId = resolveParentId() ?: return ResponseEntity.ok(emptyList())
        val profiles = voiceRepository.findByParentId(parentId).map { it.toResponse() }
        return ResponseEntity.ok(profiles)
    }

    @GetMapping("/{id}")
    fun getVoice(@PathVariable id: Long): ResponseEntity<com.tamixa.api.voice.dto.VoiceProfileResponse> {
        val parentId = resolveParentId() ?: return ResponseEntity.notFound().build()
        val profile = voiceRepository.findByIdAndParentId(id, parentId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(profile.toResponse())
    }

    /**
     * Returns the stored reference audio for one of the caller's own voice profiles.
     * 404 when the profile does not belong to the caller, has no reference audio, or storage is unavailable.
     */
    @GetMapping("/{id}/reference-audio")
    fun getReferenceAudio(@PathVariable id: Long): ResponseEntity<ByteArray> {
        val parentId = resolveParentId() ?: return ResponseEntity.notFound().build()
        val profile = voiceRepository.findByIdAndParentId(id, parentId)
            ?: return ResponseEntity.notFound().build()
        val bytes = voiceCloningService.getReferenceAudioForAdmin(parentId, id)
        if (bytes == null || bytes.isEmpty()) {
            log.info("Reference audio unavailable parentId={} voiceProfileId={}", parentId, id)
            return ResponseEntity.notFound().build()
        }
        log.info("Serving reference audio parentId={} voiceProfileId={} bytes={}", parentId, id, bytes.size)
        return ResponseEntity.ok()
            .contentType(referenceAudioMediaType(profile.referenceAudioPath))
            .contentLength(bytes.size.toLong())
            .cacheControl(CacheControl.noStore().cachePrivate())
            .body(bytes)
    }

    /**
     * Deletes one of the caller's own voice profiles: the stored recording in S3 and the profile row.
     * 404 when it isn't the caller's; 409 when a generated story still uses this voice.
     */
    @DeleteMapping("/{id}")
    fun deleteVoice(@PathVariable id: Long): ResponseEntity<Map<String, Any>> {
        val parentId = resolveParentId() ?: return ResponseEntity.notFound().build()
        voiceRepository.findByIdAndParentId(id, parentId) ?: return ResponseEntity.notFound().build()
        return try {
            voiceCloningService.deleteReferenceVoiceProfileByParentId(parentId, id)
            log.info("Parent deleted voice profile parentId={} voiceProfileId={}", parentId, id)
            ResponseEntity.noContent().build()
        } catch (e: DataIntegrityViolationException) {
            log.warn("Voice profile delete refused, used by a story parentId={} voiceProfileId={}", parentId, id)
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(mapOf("message" to "This voice is used by a story and can't be deleted", "voiceProfileId" to id))
        }
    }

    companion object {
        const val VOICE_CLONING_CONSENT = "voice_cloning"
    }

    private fun referenceAudioMediaType(path: String?): MediaType =
        when (path?.substringAfterLast('.', "")?.lowercase()) {
            "m4a", "mp4", "aac" -> MediaType.parseMediaType("audio/mp4")
            "wav" -> MediaType.parseMediaType("audio/wav")
            "mp3" -> MediaType.parseMediaType("audio/mpeg")
            "ogg" -> MediaType.parseMediaType("audio/ogg")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }

    private fun resolveParentId(): Long? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return parentRepository.findByEmail(email)?.id
    }

    private fun currentParentEmail(): String {
        return SecurityContextHolder.getContext().authentication?.name
            ?: throw IllegalStateException("Not authenticated")
    }
}
