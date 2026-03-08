package com.araro.api.stream

import com.araro.api.ApiVersion
import com.araro.api.stream.dto.FamilyVoiceUploadResponse
import com.araro.api.stream.dto.NarrationScriptResponse
import com.araro.api.stream.dto.StreamUrlResponse
import com.araro.api.stream.dto.VoiceDto
import com.araro.api.stream.dto.VoicesResponse
import com.araro.application.avatar.AvatarVideoService
import com.araro.application.avatar.FamilyAvatarService
import com.araro.application.familyvoice.FamilyVoiceService
import com.araro.application.port.ParentRepositoryPort
import com.araro.application.stream.AudioStreamService
import com.araro.application.stream.NarrationScriptService
import com.araro.application.stream.StoryVoicesService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * Story streaming and family voice endpoints.
 * GET /stories/{id}/stream-url - signed CDN URL for playback
 * GET /stories/{id}/voices - available AI voices
 * POST /stories/{id}/upload-family-voice - parent uploads recorded MP3
 * DELETE /stories/{id}/delete-family-voice - parent deletes recording
 */
@RestController
@RequestMapping("${ApiVersion.V1}/stories")
@PreAuthorize("hasRole('PARENT')")
class StreamUrlController(
    private val audioStreamService: AudioStreamService,
    private val storyVoicesService: StoryVoicesService,
    private val familyVoiceService: FamilyVoiceService,
    private val narrationScriptService: NarrationScriptService,
    private val familyAvatarService: FamilyAvatarService,
    private val avatarVideoService: AvatarVideoService,
    private val parentRepository: ParentRepositoryPort
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/curated/{id}/stream-url")
    fun getCuratedStreamUrl(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) voiceProfile: String?,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<StreamUrlResponse> {
        val parentId = resolveParentId(user)
        val url = when {
            voiceProfile != null && voiceProfile.equals("family", ignoreCase = true) && parentId != null ->
                audioStreamService.getFamilyVoiceStreamUrl(id, language, parentId)
            voiceProfile != null && voiceProfile.isNotBlank() ->
                audioStreamService.getCuratedNarrationStreamUrl(id, language, voiceProfile, parentId)
            else -> audioStreamService.getCuratedStreamUrl(id, language, parentId)
        }
        val avatarUrl = parentId?.let { familyAvatarService.getAvatarUrl(it) }
        val voice = voiceProfile?.takeIf { it.isNotBlank() } ?: "default"
        val avatarVideoUrl = parentId?.let {
            avatarVideoService.getAvatarVideoUrl(id, "curated", it, language, voice)
        }
        return if (url != null) ResponseEntity.ok(StreamUrlResponse(url, avatarUrl, avatarVideoUrl))
        else ResponseEntity.notFound().build()
    }

    @GetMapping("/generated/{id}/stream-url")
    fun getGeneratedStreamUrl(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<StreamUrlResponse> {
        val parentId = resolveParentId(user)
        val url = audioStreamService.getGeneratedStreamUrl(id, language, parentId)
        val avatarUrl = parentId?.let { familyAvatarService.getAvatarUrl(it) }
        val avatarVideoUrl = parentId?.let {
            avatarVideoService.getAvatarVideoUrl(id, "generated", it, language, "default")
        }
        return if (url != null) ResponseEntity.ok(StreamUrlResponse(url, avatarUrl, avatarVideoUrl))
        else ResponseEntity.notFound().build()
    }

    /**
     * Unified endpoint: tries curated first, then generated.
     * GET /stories/{id}/stream-url?language=ta&voiceProfile=calm
     */
    @GetMapping("/{id}/stream-url")
    fun getStreamUrl(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) voiceProfile: String?,
        @RequestParam(required = false) storySource: String?,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<StreamUrlResponse> {
        val parentId = resolveParentId(user)
        val curatedUrl = when {
            voiceProfile != null && voiceProfile.equals("family", ignoreCase = true) && parentId != null ->
                audioStreamService.getFamilyVoiceStreamUrl(id, language, parentId)
            voiceProfile != null && voiceProfile.isNotBlank() ->
                audioStreamService.getCuratedNarrationStreamUrl(id, language, voiceProfile, parentId)
            else -> audioStreamService.getCuratedStreamUrl(id, language, parentId)
        }
        val url = curatedUrl ?: audioStreamService.getGeneratedStreamUrl(id, language, parentId)
        val effectiveSource = storySource?.takeIf { it in listOf("curated", "generated") }
            ?: if (curatedUrl != null) "curated" else "generated"
        val voice = voiceProfile?.takeIf { it.isNotBlank() } ?: "default"
        val avatarUrl = parentId?.let { familyAvatarService.getAvatarUrl(it) }
        val avatarVideoUrl = parentId?.let {
            avatarVideoService.getAvatarVideoUrl(id, effectiveSource, it, language, voice)
        }
        return if (url != null) ResponseEntity.ok(StreamUrlResponse(url, avatarUrl, avatarVideoUrl))
        else ResponseEntity.notFound().build()
    }

    /**
     * GET /stories/{id}/narration-script?language=ta
     * Returns conversational (rewritten) script for TTS fallback.
     * Used when backend audio stream fails—mobile can synthesize this with device TTS for warm narration.
     */
    @GetMapping("/{id}/narration-script")
    fun getNarrationScript(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String
    ): ResponseEntity<NarrationScriptResponse> {
        val script = narrationScriptService.getNarrationScript(id, language)
        return if (script != null) ResponseEntity.ok(NarrationScriptResponse(script))
        else ResponseEntity.notFound().build()
    }

    /**
     * GET /stories/{id}/voices?language=ta
     * Returns available voices for the story with premium flags.
     */
    @GetMapping("/{id}/voices")
    fun getAvailableVoices(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<VoicesResponse> {
        return try {
            val parentId = resolveParentId(user)
            val voices = storyVoicesService.getAvailableVoices(id, language, parentId)
            ResponseEntity.ok(VoicesResponse(voices = voices.map { VoiceDto(it.voiceProfile, it.isPremium) }))
        } catch (e: Exception) {
            log.warn("getAvailableVoices(storyId={}, language={}) failed", id, language, e)
            ResponseEntity.ok(VoicesResponse(voices = emptyList()))
        }
    }

    @PostMapping("/{id}/upload-family-voice", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadFamilyVoice(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<FamilyVoiceUploadResponse> {
        if (file.isEmpty) return ResponseEntity.badRequest().build()
        val email = currentParentEmail()
        val voice = familyVoiceService.uploadFamilyVoice(email, id, language, file.bytes)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            FamilyVoiceUploadResponse(storyId = id, language = voice.language, uploadedAt = voice.createdAt.toString())
        )
    }

    @DeleteMapping("/{id}/delete-family-voice")
    fun deleteFamilyVoice(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String
    ): ResponseEntity<Unit> {
        val email = currentParentEmail()
        familyVoiceService.deleteFamilyVoice(email, id, language)
        return ResponseEntity.ok().build()
    }

    private fun resolveParentId(user: UserDetails?): Long? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return parentRepository.findByEmail(email)?.id
    }

    private fun currentParentEmail(): String {
        return SecurityContextHolder.getContext().authentication?.name
            ?: throw IllegalStateException("Not authenticated")
    }
}
