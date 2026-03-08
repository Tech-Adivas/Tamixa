package com.araro.api.voice

import com.araro.api.ApiVersion
import com.araro.application.port.ParentRepositoryPort
import com.araro.application.voice.VoiceCloningService
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
    private val voiceRepository: com.araro.application.port.VoiceRepositoryPort,
    private val parentRepository: ParentRepositoryPort
) {

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadVoice(@RequestParam("file") file: MultipartFile): ResponseEntity<com.araro.api.voice.dto.VoiceProfileResponse> {
        if (file.isEmpty) return ResponseEntity.badRequest().build()
        val email = currentParentEmail()
        val profile = voiceCloningService.uploadVoice(email, file.bytes, file.originalFilename ?: "voice.mp3")
        return ResponseEntity.ok(profile.toResponse())
    }

    @GetMapping
    fun listVoices(): ResponseEntity<List<com.araro.api.voice.dto.VoiceProfileResponse>> {
        val parentId = resolveParentId() ?: return ResponseEntity.ok(emptyList())
        val profiles = voiceRepository.findByParentId(parentId).map { it.toResponse() }
        return ResponseEntity.ok(profiles)
    }

    @GetMapping("/{id}")
    fun getVoice(@PathVariable id: Long): ResponseEntity<com.araro.api.voice.dto.VoiceProfileResponse> {
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
