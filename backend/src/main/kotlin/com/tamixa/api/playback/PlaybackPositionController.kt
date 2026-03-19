package com.tamixa.api.playback

import com.tamixa.api.ApiVersion
import com.tamixa.api.playback.dto.PositionResponse
import com.tamixa.api.playback.dto.SavePositionRequest
import com.tamixa.application.playback.PlaybackPositionService
import com.tamixa.infrastructure.persistence.ParentJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/playback")
@PreAuthorize("hasRole('PARENT')")
class PlaybackPositionController(
    private val playbackPositionService: PlaybackPositionService,
    private val parentJpaRepository: ParentJpaRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/position")
    fun savePosition(@jakarta.validation.Valid @RequestBody request: SavePositionRequest): ResponseEntity<Unit> {
        val parentId = currentParentId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        log.debug("Playback savePosition parentId={} storyId={} position={}s", parentId, request.storyId, request.positionSeconds)
        playbackPositionService.savePosition(
            parentId = parentId,
            storyId = request.storyId,
            storySource = request.storySource,
            positionSeconds = request.positionSeconds,
            childId = request.childId
        )
        return ResponseEntity.ok().build()
    }

    @GetMapping("/position")
    fun getPosition(
        @RequestParam storyId: Long,
        @RequestParam(defaultValue = "generated") storySource: String
    ): ResponseEntity<PositionResponse> {
        val parentId = currentParentId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        log.debug("Playback getPosition parentId={} storyId={} source={}", parentId, storyId, storySource)
        val pos = playbackPositionService.getPosition(parentId, storyId, storySource)
        return ResponseEntity.ok(PositionResponse(positionSeconds = pos ?: 0))
    }

    @GetMapping("/recent")
    fun getRecent(
        @RequestParam(defaultValue = "10") limit: Int,
        @RequestParam(defaultValue = "false") enriched: Boolean
    ): ResponseEntity<Any> {
        val parentId = currentParentId() ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        log.debug("Playback getRecent parentId={} limit={} enriched={}", parentId, limit, enriched)
        return if (enriched) {
            ResponseEntity.ok(playbackPositionService.getRecentEnriched(parentId, limit))
        } else {
            ResponseEntity.ok(playbackPositionService.getRecentWithPositions(parentId, limit))
        }
    }

    private fun currentParentId(): Long? {
        val email = SecurityContextHolder.getContext().authentication?.name ?: return null
        return parentJpaRepository.findByEmail(email)?.id
    }
}
