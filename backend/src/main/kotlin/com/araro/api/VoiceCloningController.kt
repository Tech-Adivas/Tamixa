package com.araro.api

import com.araro.api.dto.CreateVoiceCloningJobRequest
import com.araro.api.dto.VoiceCloningJobDto
import com.araro.api.dto.VoiceTierDto
import com.araro.application.subscription.SubscriptionTierService
import com.araro.application.voice.VoiceCloningService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/voice-cloning")
class VoiceCloningController(
    private val voiceCloningService: VoiceCloningService,
    private val subscriptionTierService: SubscriptionTierService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    @PreAuthorize("hasRole('PARENT')")
    fun getVoiceCloningJobs(): ResponseEntity<List<VoiceCloningJobDto>> {
        val jobs = voiceCloningService.getVoiceCloningJobs(getCurrentParentId())
            .map { toDto(it) }
        return ResponseEntity.ok(jobs)
    }

    @GetMapping("/{jobId}")
    @PreAuthorize("hasRole('PARENT')")
    fun getVoiceCloningJob(@PathVariable jobId: Long): ResponseEntity<VoiceCloningJobDto> {
        val currentParentId = getCurrentParentId()
        val job = voiceCloningService.getVoiceCloningJob(jobId)
            ?: return ResponseEntity.notFound().build()
        
        if (job.parentId != currentParentId) {
            return ResponseEntity.status(403).build()
        }
        
        return ResponseEntity.ok(toDto(job))
    }

    @GetMapping("/ready")
    @PreAuthorize("hasRole('PARENT')")
    fun getReadyVoices(): ResponseEntity<List<VoiceCloningJobDto>> {
        val voices = voiceCloningService.getReadyVoices(getCurrentParentId())
            .map { toDto(it) }
        return ResponseEntity.ok(voices)
    }

    @PostMapping
    @PreAuthorize("hasRole('PARENT')")
    fun createVoiceCloningJob(@RequestBody request: CreateVoiceCloningJobRequest): ResponseEntity<VoiceCloningJobDto> {
        val job = voiceCloningService.createVoiceCloningJob(
            parentId = getCurrentParentId(),
            audioStoragePath = request.audioStoragePath,
            audioFileSizeBytes = request.audioFileSizeBytes,
            voiceName = request.voiceName
        )
        // Trigger async processing
        voiceCloningService.processVoiceCloningJob(job.id)
        return ResponseEntity.ok(toDto(job))
    }

    @GetMapping("/tiers")
    fun getVoiceTiers(): ResponseEntity<List<VoiceTierDto>> {
        val tiers = subscriptionTierService.getAllTiers()
            .map { toDto(it) }
        return ResponseEntity.ok(tiers)
    }

    private fun toDto(job: com.araro.domain.VoiceCloningJob): VoiceCloningJobDto {
        return VoiceCloningJobDto(
            id = job.id,
            parentId = job.parentId,
            audioStoragePath = job.audioStoragePath,
            audioFileSizeBytes = job.audioFileSizeBytes,
            voiceName = job.voiceName,
            elevenLabsVoiceId = job.elevenLabsVoiceId,
            status = job.status,
            errorMessage = job.errorMessage,
            createdAt = job.createdAt.toString(),
            completedAt = job.completedAt?.toString()
        )
    }

    private fun toDto(tier: com.araro.domain.SubscriptionTier): VoiceTierDto {
        return VoiceTierDto(
            id = tier.id,
            name = tier.name,
            priceMonthly = tier.priceMonthly,
            priceYearly = tier.priceYearly,
            maxChildren = tier.maxChildren,
            maxVoices = tier.maxVoices,
            maxAvatarVideos = tier.maxAvatarVideos,
            maxSoundscapes = tier.maxSoundscapes,
            allowsVoiceCloning = tier.allowsVoiceCloning,
            allowsAvatarVideo = tier.allowsAvatarVideo,
            allowsSoundscapes = tier.allowsSoundscapes,
            allowsFamilySharing = tier.allowsFamilySharing,
            analyticsEnabled = tier.analyticsEnabled
        )
    }

    private fun getCurrentParentId(): Long {
        // In production, extract from JWT token
        return 1L // Placeholder
    }
}
