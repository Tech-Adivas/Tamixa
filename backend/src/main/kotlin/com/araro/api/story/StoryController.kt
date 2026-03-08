package com.araro.api.story

import com.araro.api.story.dto.GenerateStoryRequest
import com.araro.api.story.dto.StoryResponse
import com.araro.application.curated.CuratedStoryService
import com.araro.application.stream.CoverImageUrlResolver
import com.araro.application.story.StoryService
import com.araro.domain.Story
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
import com.araro.api.ApiVersion

@RestController
@RequestMapping("${ApiVersion.V1}/stories")
@PreAuthorize("hasRole('PARENT')")
class StoryController(
    private val storyService: StoryService,
    private val curatedStoryService: CuratedStoryService,
    private val coverImageUrlResolver: CoverImageUrlResolver
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
                content = result.content.map { s -> s.toResponse(coverImageUrlResolver.resolveCoverUrl(s)) },
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
        val curatedPage = curatedStoryService.search(q, language, page, size)
        val generatedPage = storyService.search(parentEmail, q, page, size)
        val curatedItems = curatedPage.content.map { c ->
            SearchStoryItem(
                storyId = c.id,
                storySource = "curated",
                title = c.title,
                theme = c.theme,
                language = c.language,
                age = c.age,
                childName = c.childName,
                wordCount = c.wordCount,
                readingTimeMinutes = c.readingTimeMinutes,
                coverImageUrl = c.coverImageUrl,
                coverVideoUrl = c.coverVideoUrl,
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
                coverImageUrl = coverImageUrlResolver.resolveCoverUrl(g),
                status = g.status.name
            )
        }
        val content = curatedItems + generatedItems
        return ResponseEntity.ok(
            SearchStoriesResponse(
                content = content,
                totalElements = curatedPage.totalElements + generatedPage.totalElements
            )
        )
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: Long): ResponseEntity<StoryResponse?> {
        val parentEmail = currentParentEmail()
        val story = storyService.findByParentAndId(parentEmail, id)
        return ResponseEntity.ok(story.toResponse(coverImageUrlResolver.resolveCoverUrl(story)))
    }

    @PostMapping("/generate")
    fun generate(@Valid @RequestBody request: GenerateStoryRequest): ResponseEntity<StoryResponse> {
        val parentEmail = currentParentEmail()
        log.info("Story generate request theme={} age={}", request.theme, request.age)
        val language = request.language.trim().lowercase()
        if (language != "ta" && language != "tamil") {
            log.warn("Story generate rejected: unsupported language={}", language)
            throw IllegalArgumentException("AI story generation is currently Tamil only")
        }
        val story = storyService.generate(
            parentEmail = parentEmail,
            age = request.age,
            language = "ta",
            theme = request.theme,
            childName = request.childName,
            childId = request.childId,
            emotionMode = request.emotionMode,
            parentCustomPrompt = request.parentCustomPrompt,
            conversationMessages = request.conversationMessages
        )
        log.info("Story generate success storyId={}", story.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(story.toResponse(coverImageUrlResolver.resolveCoverUrl(story)))
    }

    private fun currentParentEmail(): String {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Not authenticated")
        return auth.name
    }
}

data class StoriesPageResponse(
    val content: List<StoryResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val first: Boolean,
    val last: Boolean
)

data class SearchStoryItem(
    val storyId: Long,
    val storySource: String,
    val title: String?,
    val theme: String,
    val language: String,
    val age: Int,
    val childName: String,
    val wordCount: Int,
    val readingTimeMinutes: Double,
    val coverImageUrl: String?,
    val coverVideoUrl: String? = null,
    val status: String
)

data class SearchStoriesResponse(
    val content: List<SearchStoryItem>,
    val totalElements: Long
)
