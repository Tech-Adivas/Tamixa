package com.tamixa.api.story

import com.tamixa.api.story.dto.GenerateStoryRequest
import com.tamixa.api.story.dto.GenerationTopicResponse
import com.tamixa.application.story.StoryGenerationTopicRegistry
import com.tamixa.api.story.dto.RemixRequest
import com.tamixa.api.story.dto.SearchStoriesResponse
import com.tamixa.api.story.dto.SearchStoryItem
import com.tamixa.api.story.dto.StoriesPageResponse
import com.tamixa.api.story.dto.StoryResponse
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.application.stream.CoverImageUrlResolver
import com.tamixa.application.story.StoryIllustrationService
import com.tamixa.application.story.StoryService
import com.tamixa.domain.Story
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import com.tamixa.api.ApiVersion
import com.tamixa.api.exception.ApiBadRequestException
import com.tamixa.api.exception.ApiErrorCodes

@RestController
@RequestMapping("${ApiVersion.V1}/stories")
@PreAuthorize("hasRole('PARENT')")
class StoryController(
    private val storyService: StoryService,
    private val storyLibraryService: StoryLibraryService,
    private val coverImageUrlResolver: CoverImageUrlResolver,
    private val storyIllustrationService: StoryIllustrationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<StoriesPageResponse> {
        val parentEmail = currentParentEmail()
        log.debug("Stories list page={} size={}", page, size)
        val result = storyService.findByParent(parentEmail, page, size)
        log.debug("Stories list returned count={} totalElements={}", result.content.size, result.totalElements)
        return ResponseEntity.ok(
            StoriesPageResponse(
                content = result.content.map { s -> s.toResponse(coverImageUrlResolver) },
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                first = result.isFirst,
                last = result.isLast
            )
        )
    }

    @GetMapping("/search")
    fun search(
        @RequestParam q: String,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<SearchStoriesResponse> {
        val parentEmail = currentParentEmail()
        log.debug("Stories search q={} language={}", q, language)
        val libraryPage = storyLibraryService.search(q, language, page, size)
        val generatedPage = storyService.search(parentEmail, q, page, size)
        val libraryItems = libraryPage.content.map { c ->
            SearchStoryItem(
                storyId = c.id,
                storySource = "library",
                title = c.title,
                theme = c.theme,
                language = c.language,
                age = c.age,
                childName = c.childName,
                wordCount = c.wordCount,
                readingTimeMinutes = c.readingTimeMinutes,
                coverImageUrl = c.coverImageUrl,  // Already resolved by StoryLibraryService.search()
                coverVideoUrl = c.coverVideoUrl,  // Already resolved by StoryLibraryService.search()
                status = c.status
            )
        }
        val generatedItems = generatedPage.content.map { g ->
            SearchStoryItem(
                storyId = g.id,
                storySource = "generated",
                title = g.title,
                theme = g.theme,
                language = g.language,
                age = g.age,
                childName = g.childName,
                wordCount = g.wordCount,
                readingTimeMinutes = g.readingTimeMinutes,
                coverImageUrl = coverImageUrlResolver.resolveCoverPath(g.coverImageUrl),
                coverVideoUrl = coverImageUrlResolver.resolveCoverVideoPath(g.coverVideoUrl),
                status = g.status.name
            )
        }
        val content = libraryItems + generatedItems
        return ResponseEntity.ok(
            SearchStoriesResponse(
                content = content,
                totalElements = libraryPage.totalElements + generatedPage.totalElements
            )
        )
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<StoryResponse?> {
        val parentEmail = currentParentEmail()
        val story = storyService.findByParentAndId(parentEmail, id)
        return ResponseEntity.ok(story.toResponse(coverImageUrlResolver))
    }

    @GetMapping("/generation-topics")
    fun listGenerationTopics(): ResponseEntity<List<GenerationTopicResponse>> {
        val body = StoryGenerationTopicRegistry.all().map {
            GenerationTopicResponse(
                id = it.id,
                theme = it.theme,
                suggestedLearningFocus = it.suggestedLearningFocus,
                descriptionEn = it.descriptionEn,
            )
        }
        return ResponseEntity.ok(body)
    }

    @PostMapping("/generate")
    fun generate(@Valid @RequestBody request: GenerateStoryRequest): ResponseEntity<StoryResponse> {
        val parentEmail = currentParentEmail()
        val topicId = request.generationTopicId?.trim()?.takeIf { it.isNotBlank() }
        val topic = topicId?.let { StoryGenerationTopicRegistry.resolve(it) }
        if (topicId != null && topic == null) {
            throw ApiBadRequestException(ApiErrorCodes.UNKNOWN_GENERATION_TOPIC, "Unknown generationTopicId")
        }
        if (topic == null && request.theme.isNullOrBlank()) {
            throw ApiBadRequestException(ApiErrorCodes.THEME_OR_TOPIC_REQUIRED, "Provide theme or generationTopicId")
        }
        val effectiveTheme = topic?.theme ?: request.theme!!.trim()
        val trustedCatalog = topic != null
        val effectiveLearning = request.learningFocus?.takeIf { it.isNotBlank() } ?: topic?.suggestedLearningFocus
        log.info("Story generate request themeResolved={} age={} topicId={}", effectiveTheme, request.age, request.generationTopicId)
        val language = request.language.trim().lowercase()
        if (language != "ta" && language != "tamil") {
            log.warn("Story generate rejected: unsupported language={}", language)
            throw ApiBadRequestException(
                ApiErrorCodes.GENERATION_LANGUAGE_NOT_SUPPORTED,
                "AI story generation is currently Tamil only",
            )
        }
        val effectiveChildName = request.childName?.takeIf { it.isNotBlank() } ?: "Listener"
        val story = storyService.generate(
            parentEmail = parentEmail,
            age = request.age,
            language = "ta",
            theme = effectiveTheme,
            childName = effectiveChildName,
            childId = request.childId,
            emotionMode = request.emotionMode,
            parentCustomPrompt = request.parentCustomPrompt,
            conversationMessages = request.conversationMessages,
            learningFocus = effectiveLearning,
            trustedCatalogTheme = trustedCatalog,
        )
        log.info("Story generate success storyId={}", story.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(story.toResponse(coverImageUrlResolver))
    }

    /**
     * Parent-facing: regenerate AI cover for a generated story they own.
     * Prefers animated GIF when image-to-video is configured (then FFmpeg), else static cover image. Tamil Nadu/South India theme, HD.
     * Always deletes the old cover then generates a new one.
     */
    @PostMapping("/{id}/regenerate-cover")
    fun regenerateCover(
        @PathVariable id: Long,
        @org.springframework.web.bind.annotation.RequestParam(defaultValue = "true") force: Boolean
    ): ResponseEntity<StoryResponse> {
        val parentEmail = currentParentEmail()
        val story = storyService.findByParentAndId(parentEmail, id)
        val updated = storyIllustrationService.generateCoverForStory(story, force)
        return if (updated != null) {
            ResponseEntity.ok(updated.toResponse(coverImageUrlResolver))
        } else {
            log.warn("Regenerate cover failed for story id={}", id)
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
        }
    }

    /**
     * Parent-facing: remix a story with an instruction (e.g. "make the dragon friendly").
     * Returns 501 until remix use case is implemented.
     */
    @PostMapping("/{id}/remix")
    fun remix(
        @PathVariable id: Long,
        @Valid @RequestBody body: RemixRequest
    ): ResponseEntity<StoryResponse> {
        val parentEmail = currentParentEmail()
        storyService.findByParentAndId(parentEmail, id)
        log.debug("Remix requested for story id={} (not yet implemented)", id)
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()
    }

    private fun currentParentEmail(): String {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Not authenticated")
        return auth.name
    }
}
