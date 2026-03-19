package com.tamixa.api.story

import com.tamixa.api.ApiVersion
import com.tamixa.api.stream.dto.StreamUrlResponse
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.stream.AudioStreamService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * User-facing API: stream URL for stories.
 * GET /stories/{id}/stream?language=&voice= - signed URL (JWT + subscription validated)
 * Library list is at /stories/library; my stories at GET /stories (StoryController)
 */
@RestController
@RequestMapping("${ApiVersion.V1}/stories")
@PreAuthorize("hasRole('PARENT')")
class StoriesApiController(
    private val audioStreamService: AudioStreamService,
    private val parentRepository: ParentRepositoryPort
) {

    @GetMapping("/{id}/stream")
    fun getStreamUrl(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false, defaultValue = "default") voice: String,
        @RequestParam(required = false) voiceProfile: String?
    ): ResponseEntity<StreamUrlResponse> {
        val parentId = resolveParentId()
        val effectiveVoice = voiceProfile ?: voice
        val url = when {
            effectiveVoice.equals("family", ignoreCase = true) && parentId != null ->
                audioStreamService.getFamilyVoiceStreamUrl(id, language, parentId)
            effectiveVoice.isNotBlank() ->
                audioStreamService.getLibraryNarrationStreamUrl(id, language, effectiveVoice, parentId)
            else ->
                audioStreamService.getLibraryStreamUrl(id, language, parentId)
        } ?: audioStreamService.getGeneratedStreamUrl(id, language, parentId)
        return if (url != null) ResponseEntity.ok(StreamUrlResponse(url))
        else ResponseEntity.notFound().build()
    }

    private fun resolveParentId(): Long? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return parentRepository.findByEmail(email)?.id
    }
}
