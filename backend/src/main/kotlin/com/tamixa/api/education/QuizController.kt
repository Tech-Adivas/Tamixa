package com.tamixa.api.education

import com.tamixa.api.ApiVersion
import com.tamixa.api.education.dto.QuizResponse
import com.tamixa.api.education.dto.QuizResultResponse
import com.tamixa.api.education.dto.QuizResultsPageResponse
import com.tamixa.api.education.dto.SubmitQuizRequest
import com.tamixa.application.service.QuizService
import com.tamixa.application.story.StoryService
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

@RestController
@RequestMapping("${ApiVersion.V1}/quizzes")
@PreAuthorize("hasRole('PARENT')")
class QuizController(
    private val quizService: QuizService,
    private val storyService: StoryService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/stories/{storyId}")
    fun getQuizForStory(@PathVariable storyId: Long): ResponseEntity<QuizResponse> {
        log.debug("Get quiz for story={}", storyId)
        val parentEmail = currentParentEmail()
        val story = storyService.findByParentAndId(parentEmail, storyId)
            ?: return ResponseEntity.notFound().build()
        
        val quiz = quizService.getOrGenerateQuizForStory(story)
        return ResponseEntity.ok(QuizResponse.from(quiz))
    }

    @PostMapping("/{quizId}/submit")
    fun submitQuiz(
        @PathVariable quizId: Long,
        @Valid @RequestBody request: SubmitQuizRequest,
        @RequestParam childId: Long
    ): ResponseEntity<QuizResultResponse> {
        log.info("Submit quiz: quiz={} child={}", quizId, childId)
        val result = quizService.submitQuizResponse(quizId, childId, request.answers)
        return ResponseEntity.status(HttpStatus.CREATED).body(QuizResultResponse.from(result))
    }

    @GetMapping("/results")
    fun getQuizResults(
        @RequestParam childId: Long,
        @RequestParam(defaultValue = "20") limit: Int
    ): ResponseEntity<QuizResultsPageResponse> {
        log.debug("Get quiz results for child={}", childId)
        val results = quizService.getQuizResultsForChild(childId, limit)
        return ResponseEntity.ok(
            QuizResultsPageResponse(
                content = results.map { QuizResultResponse.from(it) },
                totalElements = results.size.toLong(),
                totalPages = 1,
                page = 0,
                first = true,
                last = true
            )
        )
    }

    private fun currentParentEmail(): String {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Not authenticated")
        return auth.name
    }
}
