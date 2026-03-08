package com.araro.api

import com.araro.api.dto.CreateSoundscapeRequest
import com.araro.api.dto.SoundscapeDto
import com.araro.api.dto.SoundscapeUsageDto
import com.araro.application.soundscape.SoundscapeService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/soundscapes")
class SoundscapeController(
    private val soundscapeService: SoundscapeService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun getAllSoundscapes(): ResponseEntity<List<SoundscapeDto>> {
        val soundscapes = soundscapeService.getAllSoundscapes()
            .map { toDto(it) }
        return ResponseEntity.ok(soundscapes)
    }

    @GetMapping("/category/{category}")
    fun getSoundscapesByCategory(@PathVariable category: String): ResponseEntity<List<SoundscapeDto>> {
        val soundscapeCategory = try {
            com.araro.domain.SoundscapeCategory.valueOf(category.uppercase())
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
        
        val soundscapes = soundscapeService.getSoundscapesByCategory(soundscapeCategory)
            .map { toDto(it) }
        return ResponseEntity.ok(soundscapes)
    }

    @GetMapping("/search")
    fun searchSoundscapes(@RequestParam name: String): ResponseEntity<List<SoundscapeDto>> {
        val soundscapes = soundscapeService.searchSoundscapes(name)
            .map { toDto(it) }
        return ResponseEntity.ok(soundscapes)
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createSoundscape(@RequestBody request: CreateSoundscapeRequest): ResponseEntity<SoundscapeDto> {
        // In production, this would accept file upload
        val soundscape = soundscapeService.createSoundscape(
            name = request.name,
            category = request.category,
            audioBytes = byteArrayOf(), // Placeholder
            description = request.description,
            durationSeconds = request.durationSeconds
        )
        return ResponseEntity.ok(toDto(soundscape))
    }

    @PostMapping("/{soundscapeId}/use")
    @PreAuthorize("hasRole('PARENT')")
    fun useSoundscape(
        @PathVariable soundscapeId: Long,
        @RequestParam storyId: Long? = null
    ): ResponseEntity<Void> {
        val currentParentId = getCurrentParentId()
        
        // Verify soundscape exists
        val soundscape = soundscapeService.getSoundscapesById(soundscapeId)
            ?: return ResponseEntity.notFound().build()
        
        soundscapeService.useSoundscape(
            parentId = currentParentId,
            storyId = storyId,
            soundscapeId = soundscapeId
        )
        return ResponseEntity.ok().build()
    }

    @GetMapping("/usage")
    @PreAuthorize("hasRole('PARENT')")
    fun getSoundscapeUsage(): ResponseEntity<List<SoundscapeUsageDto>> {
        val usages = soundscapeService.getSoundscapeUsage(getCurrentParentId())
            .map { toUsageDto(it) }
        return ResponseEntity.ok(usages)
    }

    private fun toDto(soundscape: com.araro.domain.Soundscape): SoundscapeDto {
        return SoundscapeDto(
            id = soundscape.id,
            name = soundscape.name,
            category = soundscape.category,
            durationSeconds = soundscape.durationSeconds,
            audioUrl = soundscape.audioUrl,
            description = soundscape.description
        )
    }

    private fun toUsageDto(usage: com.araro.domain.SoundscapeUsage): SoundscapeUsageDto {
        return SoundscapeUsageDto(
            id = usage.id,
            parentId = usage.parentId,
            storyId = usage.storyId,
            soundscapeId = usage.soundscapeId,
            soundscapeName = "Unknown", // Would need soundscape lookup
            usageCount = usage.usageCount,
            lastUsedAt = usage.lastUsedAt.toString()
        )
    }

    private fun getCurrentParentId(): Long {
        // In production, extract from JWT token
        return 1L // Placeholder
    }
}
