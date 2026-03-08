package com.araro.api.curated

import com.araro.api.ApiVersion
import com.araro.application.curated.CuratedStoryService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Parent-facing API for curated stories.
 * Tamil (ta): from master. Other languages: from story_translations + story_audio.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/stories/curated")
@PreAuthorize("hasRole('PARENT')")
class CuratedStoryController(
    private val curatedStoryService: CuratedStoryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun list(
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int
    ): ResponseEntity<List<com.araro.api.admin.dto.CuratedStoryResponse>> {
        log.debug("CuratedStory list language={} page={} size={}", language, page, size)
        val result = curatedStoryService.findByLanguage(language.trim().lowercase(), page, size)
        return ResponseEntity.ok(result.content)
    }

    @GetMapping("/{id}")
    fun getById(
        @PathVariable id: Long,
        @RequestParam(defaultValue = "ta") language: String
    ): ResponseEntity<com.araro.api.admin.dto.CuratedStoryResponse?> {
        log.debug("CuratedStory getById id={} language={}", id, language)
        val story = curatedStoryService.findByIdAndLanguage(id, language.trim().lowercase())
        return if (story != null) ResponseEntity.ok(story)
        else {
            log.debug("CuratedStory not found id={} language={}", id, language)
            ResponseEntity.notFound().build()
        }
    }
}
