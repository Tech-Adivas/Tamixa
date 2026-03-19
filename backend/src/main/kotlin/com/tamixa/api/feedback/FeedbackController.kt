package com.tamixa.api.feedback

import com.tamixa.api.ApiVersion
import com.tamixa.application.feedback.FeedbackService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${ApiVersion.V1}/feedback")
@PreAuthorize("hasRole('PARENT')")
class FeedbackController(
    private val feedbackService: FeedbackService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun submit(@Valid @RequestBody request: FeedbackRequest): ResponseEntity<Unit> {
        val email = SecurityContextHolder.getContext().authentication?.name ?: throw IllegalStateException("Not authenticated")
        log.info("Feedback submit storyId={} storySource={} rating={}", request.storyId, request.storySource, request.rating)
        feedbackService.submit(email, request.storyId, request.storySource, request.rating, request.comment)
        return ResponseEntity.ok().build()
    }
}

data class FeedbackRequest(
    val storyId: Long? = null,
    val storySource: String? = "generated",
    val rating: Int? = null,
    val comment: String? = null
)
