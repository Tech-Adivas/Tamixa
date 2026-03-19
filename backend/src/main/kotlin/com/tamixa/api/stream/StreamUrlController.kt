package com.tamixa.api.stream

import com.tamixa.api.ApiVersion
import com.tamixa.api.stream.dto.AvatarVideoStatusResponse
import com.tamixa.api.stream.dto.FamilyVoiceUploadResponse
import com.tamixa.api.stream.dto.NarrationScriptResponse
import com.tamixa.api.stream.dto.StreamUrlResponse
import com.tamixa.api.stream.dto.VoiceDto
import com.tamixa.api.stream.dto.VoicePreferenceRequest
import com.tamixa.api.stream.dto.VoicePreferenceResponse
import com.tamixa.api.stream.dto.VoicesResponse
import com.tamixa.application.avatar.AvatarVideoService
import com.tamixa.domain.AvatarVideoStatus
import com.tamixa.application.avatar.FamilyAvatarService
import com.tamixa.application.familyvoice.FamilyVoiceService
import com.tamixa.application.playback.PlaybackManifest
import com.tamixa.application.playback.PlaybackManifestService
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.application.stream.AudioStreamService
import com.tamixa.application.stream.NarrationScriptService
import com.tamixa.application.stream.StoryVoicePreferenceService
import com.tamixa.application.stream.StoryVoicesService
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import jakarta.validation.Valid
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
    private val storyVoicePreferenceService: StoryVoicePreferenceService,
    private val familyVoiceService: FamilyVoiceService,
    private val narrationScriptService: NarrationScriptService,
    private val familyAvatarService: FamilyAvatarService,
    private val avatarVideoService: AvatarVideoService,
    private val playbackManifestService: PlaybackManifestService,
    private val parentRepository: ParentRepositoryPort,
    private val subscriptionService: SubscriptionService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/library/{id}/stream-url")
    fun getLibraryStreamUrl(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) voiceProfile: String?,
        @RequestParam(required = false) playbackMode: String?,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<StreamUrlResponse> {
        val parentId = resolveParentId(user)
        val requestAvatarVideo = (playbackMode?.trim()?.lowercase() ?: "default") == "avatar"
        val clonedRequested = voiceProfile != null && voiceProfile.startsWith("cloned:", ignoreCase = true)
        val primaryUrl = when {
            voiceProfile != null && voiceProfile.equals("family", ignoreCase = true) && parentId != null ->
                audioStreamService.getFamilyVoiceStreamUrl(id, language, parentId)
            voiceProfile != null && voiceProfile.isNotBlank() ->
                audioStreamService.getLibraryNarrationStreamUrl(id, language, voiceProfile, parentId)
            else -> audioStreamService.getLibraryStreamUrl(id, language, parentId)
        }
        val url = primaryUrl ?: if (clonedRequested) audioStreamService.getLibraryStreamUrl(id, language, parentId) else null
        val voiceFallback = clonedRequested && primaryUrl == null && url != null
        val avatarUrl = parentId?.let { familyAvatarService.getAvatarUrl(it) }
        val voice = voiceProfile?.takeIf { it.isNotBlank() } ?: "default"
        val avatarVideoUrl = if (requestAvatarVideo) {
            parentId?.let { pid ->
                if (subscriptionService.getOrCreateSubscription(pid).isEntitledToUnlimitedStories())
                    avatarVideoService.getAvatarVideoUrl(id, "library", pid, language, voice)
                else null
            }
        } else null
        val avatarStatus = if (requestAvatarVideo) {
            parentId?.let { pid ->
                if (subscriptionService.getOrCreateSubscription(pid).isEntitledToUnlimitedStories())
                    resolveAvatarStatus(avatarVideoUrl, avatarUrl, id, "library", pid, language, voice)
                else "NONE"
            } ?: "NONE"
        } else "NONE"
        val durationSeconds = if (url != null) audioStreamService.getLibraryStoryDurationSeconds(id, language, voice.takeIf { it.isNotBlank() } ?: "default") else null
        return if (url != null) ResponseEntity.ok(
            StreamUrlResponse(url, avatarUrl, avatarVideoUrl, avatarStatus, voiceFallback, durationSeconds = durationSeconds)
        ) else ResponseEntity.notFound().build()
    }

    @GetMapping("/generated/{id}/stream-url")
    fun getGeneratedStreamUrl(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) playbackMode: String?,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<StreamUrlResponse> {
        val parentId = resolveParentId(user)
        val requestAvatarVideo = (playbackMode?.trim()?.lowercase() ?: "default") == "avatar"
        val url = audioStreamService.getGeneratedStreamUrl(id, language, parentId)
        val avatarUrl = parentId?.let { familyAvatarService.getAvatarUrl(it) }
        val avatarVideoUrl = if (requestAvatarVideo) {
            parentId?.let { pid ->
                if (subscriptionService.getOrCreateSubscription(pid).isEntitledToUnlimitedStories())
                    avatarVideoService.getAvatarVideoUrl(id, "generated", pid, language, "default")
                else null
            }
        } else null
        val avatarStatus = if (requestAvatarVideo) {
            parentId?.let { pid ->
                if (subscriptionService.getOrCreateSubscription(pid).isEntitledToUnlimitedStories())
                    resolveAvatarStatus(avatarVideoUrl, avatarUrl, id, "generated", pid, language, "default")
                else "NONE"
            } ?: "NONE"
        } else "NONE"
        return if (url != null) ResponseEntity.ok(StreamUrlResponse(url, avatarUrl, avatarVideoUrl, avatarStatus))
        else ResponseEntity.notFound().build()
    }

    /**
     * Unified endpoint: tries curated first, then generated.
     * GET /stories/{id}/stream-url?language=ta&voiceProfile=calm&playbackMode=my_voice
     * Cloned voice and avatar video are always specific to this story: [id] is the story id
     * and is used for all downstream stream URL and avatar generation (no cross-story reuse).
     * playbackMode: default | my_voice | avatar. Only when "avatar" do we fetch/trigger avatar video.
     * For "my_voice" and "default", audio-only flow—no avatar generation.
     */
    @GetMapping("/{id}/stream-url")
    fun getStreamUrl(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) voiceProfile: String?,
        @RequestParam(required = false) storySource: String?,
        @RequestParam(required = false) playbackMode: String?,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<StreamUrlResponse> {
        val parentId = resolveParentId(user)
        val effectiveSource = storySource?.takeIf { it in listOf("library", "generated") }
        val effectivePlaybackMode = playbackMode?.trim()?.lowercase()?.takeIf { it in listOf("default", "my_voice", "avatar") } ?: "default"
        val requestAvatarVideo = effectivePlaybackMode == "avatar"
        val clonedRequested = voiceProfile != null && voiceProfile.startsWith("cloned:", ignoreCase = true)
        var usedClonedFallback = false
        val url = when (effectiveSource) {
            "generated" -> {
                val clonedUrl = if (clonedRequested && parentId != null) {
                    audioStreamService.getGeneratedClonedStreamUrl(id, language, voiceProfile!!, parentId)
                } else null
                if (clonedUrl != null) clonedUrl
                else if (clonedRequested) {
                    usedClonedFallback = true
                    audioStreamService.getGeneratedStreamUrl(id, language, parentId)
                } else {
                    audioStreamService.getGeneratedStreamUrl(id, language, parentId)
                }
            }
            "library" -> {
                val libraryUrl = when {
                    voiceProfile != null && voiceProfile.equals("family", ignoreCase = true) && parentId != null ->
                        audioStreamService.getFamilyVoiceStreamUrl(id, language, parentId)
                    voiceProfile != null && voiceProfile.isNotBlank() ->
                        audioStreamService.getLibraryNarrationStreamUrl(id, language, voiceProfile, parentId)
                    else -> audioStreamService.getLibraryStreamUrl(id, language, parentId)
                }
                if (libraryUrl != null) libraryUrl
                else if (clonedRequested) {
                    usedClonedFallback = true
                    audioStreamService.getLibraryStreamUrl(id, language, parentId)
                } else null
            }
            else -> {
                val curatedUrl = when {
                    voiceProfile != null && voiceProfile.equals("family", ignoreCase = true) && parentId != null ->
                        audioStreamService.getFamilyVoiceStreamUrl(id, language, parentId)
                    voiceProfile != null && voiceProfile.isNotBlank() ->
                        audioStreamService.getLibraryNarrationStreamUrl(id, language, voiceProfile, parentId)
                    else -> audioStreamService.getLibraryStreamUrl(id, language, parentId)
                }
                curatedUrl
                    ?: if (clonedRequested && parentId != null) {
                        audioStreamService.getGeneratedClonedStreamUrl(id, language, voiceProfile!!, parentId)
                    } else null
                    ?: if (clonedRequested) {
                        usedClonedFallback = true
                        audioStreamService.getLibraryStreamUrl(id, language, parentId)
                    } else null
                    ?: audioStreamService.getGeneratedStreamUrl(id, language, parentId)
            }
        }
        val voiceFallback = clonedRequested && usedClonedFallback
        val effectiveSourceForAvatar = effectiveSource ?: "generated"
        val voice = voiceProfile?.takeIf { it.isNotBlank() } ?: "default"
        val avatarUrl = parentId?.let { familyAvatarService.getAvatarUrl(it) }
        val avatarVideoUrl = if (requestAvatarVideo) {
            parentId?.let { pid ->
                if (subscriptionService.getOrCreateSubscription(pid).isEntitledToUnlimitedStories())
                    avatarVideoService.getAvatarVideoUrl(id, effectiveSourceForAvatar, pid, language, voice)
                else null
            }
        } else null
        val avatarStatus = if (requestAvatarVideo) {
            parentId?.let { pid ->
                if (subscriptionService.getOrCreateSubscription(pid).isEntitledToUnlimitedStories())
                    resolveAvatarStatus(avatarVideoUrl, avatarUrl, id, effectiveSourceForAvatar, pid, language, voice)
                else "NONE"
            } ?: "NONE"
        } else "NONE"
        val durationSeconds = if (url != null) audioStreamService.getLibraryStoryDurationSeconds(id, language, voice) else null
        return if (url != null) ResponseEntity.ok(
            StreamUrlResponse(url, avatarUrl, avatarVideoUrl, avatarStatus, voiceFallback, durationSeconds = durationSeconds)
        ) else ResponseEntity.notFound().build()
    }

    private fun resolveAvatarStatus(
        avatarVideoUrl: String?,
        avatarUrl: String?,
        storyId: Long,
        storySource: String,
        parentId: Long,
        language: String,
        voiceProfile: String
    ): String {
        if (avatarVideoUrl != null) return "VIDEO_READY"
        val (status, _) = avatarVideoService.getAvatarVideoStatus(storyId, storySource, parentId, language, voiceProfile)
            ?: return if (avatarUrl != null) "IMAGE_ONLY" else "NONE"
        return when (status) {
            AvatarVideoStatus.READY -> "VIDEO_READY"
            AvatarVideoStatus.PENDING, AvatarVideoStatus.PROCESSING -> "VIDEO_GENERATING"
            AvatarVideoStatus.FAILED -> "VIDEO_FAILED"
            else -> "NONE"
        }
    }

    /**
     * GET /stories/{id}/avatar-video-status?storySource=library&language=ta&voiceProfile=default
     * Returns talking video job status for personalization flow. Mobile polls until READY.
     */
    @GetMapping("/{id}/avatar-video-status")
    fun getAvatarVideoStatus(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "library") storySource: String,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(defaultValue = "default") voiceProfile: String,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<AvatarVideoStatusResponse> {
        val parentId = resolveParentId(user) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val effectiveSource = storySource.takeIf { it in listOf("library", "generated") } ?: "library"
        val result = avatarVideoService.getAvatarVideoStatus(id, effectiveSource, parentId, language, voiceProfile)
        return when {
            result == null -> ResponseEntity.ok(
                AvatarVideoStatusResponse("NOT_REQUESTED", null, null)
            )
            else -> {
                val (status, errorMsg) = result
                val videoUrl = if (status == com.tamixa.domain.AvatarVideoStatus.READY)
                    avatarVideoService.getAvatarVideoUrl(id, effectiveSource, parentId, language, voiceProfile)
                else null
                ResponseEntity.ok(
                    AvatarVideoStatusResponse(status!!.name, videoUrl, errorMsg)
                )
            }
        }
    }

    /**
     * GET /stories/{id}/timeline?language=ta&storySource=library&voiceProfile=default
     * Returns playback manifest: scenes, segments, audio URLs, subtitle text.
     * Per Tamixa spec: app plays audio and displays subtitles in sync with segments.
     */
    @GetMapping("/{id}/timeline")
    fun getTimeline(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) storySource: String?,
        @RequestParam(required = false) voiceProfile: String?,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<PlaybackManifest> {
        val parentId = resolveParentId(user)
        val manifest = playbackManifestService.getPlaybackManifest(
            storyId = id,
            language = language,
            storySource = storySource,
            voiceProfile = voiceProfile,
            parentId = parentId
        )
        return if (manifest != null) ResponseEntity.ok(manifest)
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
            ResponseEntity.ok(VoicesResponse(voices = voices.map { VoiceDto(it.voiceProfile, it.isPremium, it.displayLabel) }))
        } catch (e: Exception) {
            log.warn("getAvailableVoices(storyId={}, language={}) failed, returning empty list for resilience: {}", id, language, e.message, e)
            ResponseEntity.ok(VoicesResponse(voices = emptyList()))
        }
    }

    /**
     * GET /stories/{id}/voice-preference?storySource=curated
     * Returns the parent's saved voice preference for this story (e.g. "cloned:1" for "use my voice on this story").
     * When none saved, returns 200 with voiceProfile "default".
     */
    @GetMapping("/{id}/voice-preference")
    fun getVoicePreference(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "library") storySource: String,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<VoicePreferenceResponse> {
        val parentId = resolveParentId(user) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val voiceProfile = storyVoicePreferenceService.getPreference(parentId, id, storySource) ?: "default"
        val playbackMode = storyVoicePreferenceService.getPlaybackMode(parentId, id, storySource)
        return ResponseEntity.ok(VoicePreferenceResponse(voiceProfile, playbackMode))
    }

    /**
     * PUT /stories/{id}/voice-preference?storySource=curated
     * Saves the parent's voice preference for this story (e.g. "cloned:1" to use their cloned voice on this story).
     */
    @PutMapping("/{id}/voice-preference")
    fun setVoicePreference(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "library") storySource: String,
        @Valid @RequestBody request: VoicePreferenceRequest,
        @AuthenticationPrincipal user: UserDetails?
    ): ResponseEntity<VoicePreferenceResponse> {
        val parentId = resolveParentId(user) ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        val saved = storyVoicePreferenceService.setPreference(parentId, id, storySource, request.voiceProfile, request.playbackMode)
        return ResponseEntity.ok(VoicePreferenceResponse(saved.voiceProfile, saved.playbackMode))
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
