package com.tamixa.api.education

import com.tamixa.api.ApiVersion
import com.tamixa.api.education.dto.ReadingLevelAssessmentResponse
import com.tamixa.api.education.dto.ReadingLevelHistoryResponse
import com.tamixa.api.education.dto.ReadingLevelResponse
import com.tamixa.application.service.ReadingLevelService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/reading-levels")
@PreAuthorize("hasRole('PARENT')")
class ReadingLevelController(
    private val readingLevelService: ReadingLevelService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/{childId}")
    fun getReadingLevel(@PathVariable childId: Long): ResponseEntity<ReadingLevelResponse> {
        log.debug("Get reading level for child={}", childId)
        val level = readingLevelService.getOrCreateReadingLevel(childId)
        return ResponseEntity.ok(ReadingLevelResponse.from(level))
    }

    @GetMapping("/{childId}/history")
    fun getReadingLevelHistory(@PathVariable childId: Long): ResponseEntity<ReadingLevelHistoryResponse> {
        log.debug("Get reading level history for child={}", childId)
        val assessments = readingLevelService.getAssessmentHistory(childId)
        return ResponseEntity.ok(
            ReadingLevelHistoryResponse(
                assessments = assessments.map { ReadingLevelAssessmentResponse.from(it) }
            )
        )
    }
}
