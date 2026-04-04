package com.tamixa.api

import com.tamixa.api.dto.CreateVoiceCloningJobRequest
import com.tamixa.api.dto.VoiceCloningJobDto
import com.tamixa.api.dto.VoiceTierDto
import com.tamixa.application.port.ParentRepositoryPort
import com.tamixa.application.subscription.SubscriptionTierService
import com.tamixa.application.voice.VoiceCloningService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("${ApiVersion.V1}/voice-cloning")
class VoiceCloningController(
    private val voiceCloningService: VoiceCloningService,
    private val subscriptionTierService: SubscriptionTierService,
    private val parentRepository: ParentRepositoryPort
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
    fun createVoiceCloningJob(@jakarta.validation.Valid @RequestBody request: CreateVoiceCloningJobRequest): ResponseEntity<VoiceCloningJobDto> {
        val job = voiceCloningService.createVoiceCloningJob(
            parentId = getCurrentParentId(),
            audioStoragePath = request.audioStoragePath,
            consentAudioStoragePath = request.consentAudioStoragePath,
            audioFileSizeBytes = request.audioFileSizeBytes,
            voiceName = request.voiceName
        )
        // Trigger async processing
        voiceCloningService.processVoiceCloningJob(job.id)
        return ResponseEntity.ok(toDto(job))
    }

    /** Tier pricing for voice cloning; requires PARENT auth (same as other voice-cloning endpoints). */
    @GetMapping("/tiers")
    @PreAuthorize("hasRole('PARENT')")
    fun getVoiceTiers(): ResponseEntity<List<VoiceTierDto>> {
        val tiers = subscriptionTierService.getAllTiers()
            .map { toDto(it) }
        return ResponseEntity.ok(tiers)
    }

    private fun toDto(job: com.tamixa.domain.VoiceCloningJob): VoiceCloningJobDto {
        return VoiceCloningJobDto(
            id = job.id,
            parentId = job.parentId,
            audioStoragePath = job.audioStoragePath,
            audioFileSizeBytes = job.audioFileSizeBytes,
            voiceName = job.voiceName,
            elevenLabsVoiceId = job.elevenLabsVoiceId,
            fishAudioModelId = job.fishAudioModelId,
            status = job.status,
            errorMessage = job.errorMessage,
            createdAt = job.createdAt.toString(),
            completedAt = job.completedAt?.toString()
        )
    }

    private fun toDto(tier: com.tamixa.domain.SubscriptionTier): VoiceTierDto {
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
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: throw IllegalStateException("Not authenticated")
        return parentRepository.findByEmail(email)?.id
            ?: throw IllegalStateException("Parent not found for email")
    }
}
