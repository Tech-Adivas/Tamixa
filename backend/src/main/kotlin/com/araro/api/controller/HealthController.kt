package com.araro.api.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import com.araro.api.ApiVersion

/**
 * Simple health endpoint (in addition to actuator).
 * Use for readiness checks or custom health logic.
 */
@RestController
@RequestMapping(ApiVersion.V1)
class HealthController {

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "timestamp" to Instant.now().toString()
            )
        )
    }
}
