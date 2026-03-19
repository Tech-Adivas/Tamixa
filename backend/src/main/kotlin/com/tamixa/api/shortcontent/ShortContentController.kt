package com.tamixa.api.shortcontent

import com.tamixa.api.ApiVersion
import com.tamixa.api.shortcontent.dto.ShortContentResponse
import com.tamixa.application.shortcontent.ShortContentService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * Parent-facing API for short-form content: riddles, thought for the day, proverbs,
 * tongue twisters, jokes, fun facts, etc.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/short-content")
@PreAuthorize("hasRole('PARENT')")
class ShortContentController(
    private val shortContentService: ShortContentService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * List published short content by type and language (paginated).
     * type: RIDDLE, THOUGHT_FOR_THE_DAY, PROVERB, TONGUE_TWISTER, JOKE, FUN_FACT, etc.
     */
    @GetMapping
    fun list(
        @RequestParam type: String,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<List<ShortContentResponse>> {
        val result = shortContentService.findByTypeAndLanguage(type, language, page, size)
        return ResponseEntity.ok(result)
    }

    /**
     * Get the "daily" item for a type (e.g. thought for the day for today).
     * Uses display_date; if no date given, uses today.
     */
    @GetMapping("/daily")
    fun getDaily(
        @RequestParam type: String,
        @RequestParam(defaultValue = "ta") language: String,
        @RequestParam(required = false) date: LocalDate?
    ): ResponseEntity<ShortContentResponse> {
        val item = shortContentService.findDaily(type, language, date)
        return if (item != null) ResponseEntity.ok(item)
        else ResponseEntity.notFound().build()
    }

    /**
     * Returns all supported short content types (for app tabs/filters).
     */
    @GetMapping("/types")
    fun getTypes(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(shortContentService.getTypes())
    }
}
