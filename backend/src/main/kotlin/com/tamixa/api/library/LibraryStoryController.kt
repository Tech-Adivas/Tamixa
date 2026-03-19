package com.tamixa.api.library

import com.tamixa.api.ApiVersion
import com.tamixa.api.admin.dto.LibraryStoryResponse
import com.tamixa.api.admin.dto.PagedResponse
import com.tamixa.application.storylibrary.StoryLibraryService
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Parent-facing API for library stories.
 * Tamil (ta): from master. Other languages: from story_translations + story_audio.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/stories/library")
@PreAuthorize("hasRole('PARENT')")
class LibraryStoryController(
    private val storyLibraryService: StoryLibraryService,
    private val appProperties: AppProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Lists library stories approved for delivery (human-verified in admin "Story for review").
     * Only stories with narration approved appear on the app.
     * Optional theme filter (category) for browse by category.
     * Returns paginated response for load-more support.
     */
    @GetMapping
    fun list(
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(required = false) theme: String?
    ): ResponseEntity<PagedResponse<LibraryStoryResponse>> {
        log.debug("Library story list (approved only) language={} page={} size={} theme={}", language, page, size, theme)
        val result = storyLibraryService.findByLanguageApprovedOnly(language.trim().lowercase(), page, size, theme)
        val response = PagedResponse(
            content = result.content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            first = result.isFirst,
            last = result.isLast
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Returns distinct categories (themes) for approved library stories in the given language.
     * Used for browse-by-category UI.
     */
    @GetMapping("/categories")
    fun getCategories(
        @RequestParam(defaultValue = "ta") language: String
    ): ResponseEntity<CategoriesResponse> {
        val categories = storyLibraryService.getCategories(language.trim().lowercase())
        return ResponseEntity.ok(CategoriesResponse(categories = categories))
    }

    /**
     * Returns a library story only if it has been approved for delivery (human-verified).
     */
    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String
    ): ResponseEntity<com.tamixa.api.admin.dto.LibraryStoryResponse?> {
        log.debug("LibraryStory getById id={} language={}", id, language)
        val story = storyLibraryService.findByIdAndLanguage(id, language.trim().lowercase())
        if (story == null) {
            log.debug("LibraryStory not found id={} language={}", id, language)
            return ResponseEntity.notFound().build()
        }
        if (story.narrationApprovedAt == null) {
            log.debug("LibraryStory id={} not approved for delivery", id)
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(story)
    }

    /**
     * Returns shareable deep link for the story (e.g. https://tamixa.com/s/123).
     * Only for approved stories.
     */
    @GetMapping("/{id}/share-url")
    fun getShareUrl(
        @PathVariable id: Long
    ): ResponseEntity<ShareUrlResponse> {
        val story = storyLibraryService.findByIdAndLanguage(id, "ta")
        if (story == null || story.narrationApprovedAt == null) {
            return ResponseEntity.notFound().build()
        }
        val baseUrl = appProperties.shareClip.shareBaseUrl.trimEnd('/')
        val url = "$baseUrl/s/$id"
        return ResponseEntity.ok(ShareUrlResponse(url = url))
    }
}

data class ShareUrlResponse(val url: String)

data class CategoriesResponse(val categories: List<String>)
