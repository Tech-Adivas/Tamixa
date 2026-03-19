package com.tamixa.api.stream

import com.tamixa.api.ApiVersion
import com.tamixa.infrastructure.observability.StreamMetrics
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Client-reported streaming metrics.
 * POST /stories/stream/analytics
 */
@RestController
@RequestMapping("${ApiVersion.V1}/stories/stream")
@PreAuthorize("hasRole('PARENT')")
class StreamAnalyticsController(private val streamMetrics: StreamMetrics) {

    @PostMapping("/analytics")
    fun reportAnalytics(@Valid @RequestBody request: StreamAnalyticsRequest): ResponseEntity<Unit> {
        request.streamStartLatencyMs?.let { streamMetrics.recordStreamStartLatency(it) }
        if (request.bufferingEvent == true) {
            streamMetrics.recordBufferingEvent(request.storyId)
        }
        if (request.completed == true) {
            streamMetrics.recordAudioCompletion(request.storyId)
        }
        return ResponseEntity.ok().build()
    }
}

data class StreamAnalyticsRequest(
    val storyId: Long,
    @Min(0) @Max(60_000)
    val streamStartLatencyMs: Double? = null,
    val bufferingEvent: Boolean? = null,
    val completed: Boolean? = null
)
