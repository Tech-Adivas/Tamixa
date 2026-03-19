package com.tamixa.api.voice

import com.tamixa.api.ApiVersion
import com.tamixa.application.consent.ConsentService
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.voice.VoiceCloningService
import com.tamixa.infrastructure.config.AppProperties
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Voice cloning: parent uploads audio sample to create cloned voice for story narration.
 * POST /voice/upload - create voice from audio (requires ELEVENLABS_API_KEY)
 * GET /voice - list parent's voice profiles
 * GET /voice/{id} - get profile by id
 */
@RestController
@RequestMapping("${ApiVersion.V1}/voice")
@PreAuthorize("hasRole('PARENT')")
class VoiceController(
    private val voiceCloningService: VoiceCloningService,
    private val voiceRepository: com.tamixa.application.port.VoiceRepositoryPort,
    private val parentRepository: ParentRepositoryPort,
    private val appProperties: AppProperties,
    private val consentService: ConsentService
) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadVoice(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(value = "consentFile", required = false) consentFile: MultipartFile?,
        @RequestParam(value = "userConsent", required = false) userConsent: Boolean?
    ): ResponseEntity<com.tamixa.api.voice.dto.VoiceProfileResponse> {
        if (file.isEmpty) return ResponseEntity.badRequest().build()
        val email = currentParentEmail()
        val fileName = file.originalFilename ?: "voice.mp3"
        val consentBytes = consentFile?.takeIf { !it.isEmpty }?.bytes

        val voiceCloningConfig = appProperties.voiceCloning
        val useGoogle = voiceCloningConfig.enabled && voiceCloningConfig.provider == "google"
        val useElevenLabs = voiceCloningConfig.enabled && voiceCloningConfig.provider == "elevenlabs" && voiceCloningConfig.elevenLabsApiKey.isNotBlank()

        val response = when {
            useGoogle -> {
                if (consentBytes == null || consentBytes.isEmpty()) {
                    return ResponseEntity.badRequest().build()
                } else {
                    val job = voiceCloningService.uploadVoice(email, file.bytes, fileName, consentBytes)
                    voiceCloningService.processVoiceCloningJob(job.id)
                    ResponseEntity.ok(job.toResponse())
                }
            }
            useElevenLabs -> {
                val job = voiceCloningService.uploadVoice(email, file.bytes, fileName, null)
                voiceCloningService.processVoiceCloningJob(job.id)
                ResponseEntity.ok(job.toResponse())
            }
            else -> {
                val profile = voiceCloningService.createReferenceVoiceProfile(email, file.bytes, fileName)
                ResponseEntity.ok(profile.toResponse())
            }
        }
        if (userConsent == true) {
            consentService.record(email, "voice_cloning", 1)
        }
        return response
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

    private fun resolveParentId(): Long? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return parentRepository.findByEmail(email)?.id
    }

    private fun currentParentEmail(): String {
        return SecurityContextHolder.getContext().authentication?.name
            ?: throw IllegalStateException("Not authenticated")
    }
}
