package com.tamixa.api.admin

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.api.ApiVersion
import com.tamixa.application.visuals.StoryVisualsService
import com.tamixa.application.visuals.StoryVisualsView
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Story slideshow (AI pictures shown as timed camera shots during narration) for library stories.
 * Generation is started here and runs in the background; children see it only after [approve].
 */
@RestController
@Validated
@RequestMapping("${ApiVersion.V1}/admin/stories/{id}/visuals")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','CONTENT_MANAGER')")
class StoryVisualsAdminController(
    private val visuals: StoryVisualsService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun get(@PathVariable id: Long): ResponseEntity<StoryVisualsResponse> =
        visuals.adminView(id)?.let { ResponseEntity.ok(it.toResponse()) } ?: ResponseEntity.notFound().build()

    /** Plans and generates the whole slideshow (replacing any earlier one); poll [get] for progress. */
    @PostMapping("/generate")
    fun generate(@PathVariable id: Long): ResponseEntity<Any> = action(id, "generate") {
        visuals.startGeneration(id)
        ResponseEntity.status(HttpStatus.ACCEPTED).body(visuals.adminView(id)?.toResponse())
    }

    @PostMapping("/images/{imageKey}/regenerate")
    fun regenerateImage(
        @PathVariable id: Long,
        @PathVariable imageKey: String,
        @RequestBody(required = false) body: RegenerateVisualImageRequest?,
    ): ResponseEntity<Any> = action(id, "regenerate $imageKey") {
        visuals.regenerateImage(id, imageKey, body?.prompt)
        ResponseEntity.status(HttpStatus.ACCEPTED).body(visuals.adminView(id)?.toResponse())
    }

    @PostMapping("/approve")
    fun approve(@PathVariable id: Long): ResponseEntity<Any> = action(id, "approve") {
        visuals.approve(id)
        ResponseEntity.ok(visuals.adminView(id)?.toResponse())
    }

    @PostMapping("/unapprove")
    fun unapprove(@PathVariable id: Long): ResponseEntity<Any> = action(id, "unapprove") {
        visuals.unapprove(id)
        ResponseEntity.ok(visuals.adminView(id)?.toResponse())
    }

    @DeleteMapping
    fun delete(@PathVariable id: Long): ResponseEntity<Any> = action(id, "delete") {
        visuals.delete(id)
        ResponseEntity.noContent().build()
    }

    private fun action(storyId: Long, name: String, block: () -> ResponseEntity<Any>): ResponseEntity<Any> = try {
        log.info("Admin story visuals {} storyId={}", name, storyId)
        block()
    } catch (e: NoSuchElementException) {
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to e.message))
    } catch (e: IllegalStateException) {
        log.info("Admin story visuals {} refused storyId={}: {}", name, storyId, e.message)
        ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to e.message))
    } catch (e: IllegalArgumentException) {
        ResponseEntity.badRequest().body(mapOf("message" to e.message))
    }

    private fun StoryVisualsView.toResponse() = StoryVisualsResponse(
        storyId = set.storyId,
        status = set.status.name,
        running = running,
        approved = set.approvedAt != null,
        approvedAt = set.approvedAt?.toString(),
        lastError = set.lastError,
        sourceLanguage = set.sourceLanguage,
        characters = set.charactersJson?.let { runCatching { objectMapper.readTree(it) }.getOrNull() },
        characterSheetUrl = characterSheetUrl,
        images = images.map { (image, url) ->
            StoryVisualImageDto(
                imageKey = image.imageKey,
                storyMoment = image.storyMoment,
                prompt = image.prompt,
                subjects = image.subjectsJson?.let { runCatching { objectMapper.readTree(it) }.getOrNull() },
                imageUrl = url,
                status = image.status.name,
                lastError = image.lastError,
                shotCount = shots.count { it.imageKey == image.imageKey },
            )
        },
        shots = shots.map {
            StoryVisualShotDto(it.shotIndex, it.imageKey, it.narrationStart, it.startWord, it.camera, it.focus, it.movement)
        },
    )
}

data class RegenerateVisualImageRequest(
    @field:Size(max = 4000)
    val prompt: String? = null,
)

data class StoryVisualsResponse(
    val storyId: Long,
    val status: String,
    val running: Boolean,
    val approved: Boolean,
    val approvedAt: String?,
    val lastError: String?,
    val sourceLanguage: String,
    val characters: com.fasterxml.jackson.databind.JsonNode?,
    val characterSheetUrl: String?,
    val images: List<StoryVisualImageDto>,
    val shots: List<StoryVisualShotDto>,
)

data class StoryVisualImageDto(
    val imageKey: String,
    val storyMoment: String?,
    val prompt: String,
    val subjects: com.fasterxml.jackson.databind.JsonNode?,
    val imageUrl: String?,
    val status: String,
    val lastError: String?,
    val shotCount: Int,
)

data class StoryVisualShotDto(
    val shotIndex: Int,
    val imageKey: String,
    val narrationStart: String,
    val startWord: Int,
    val camera: String,
    val focus: String?,
    val movement: String,
)
