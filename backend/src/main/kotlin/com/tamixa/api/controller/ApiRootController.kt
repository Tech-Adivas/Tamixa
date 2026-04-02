package com.tamixa.api.controller

import com.tamixa.api.ApiVersion
import com.tamixa.infrastructure.config.AppProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Public root for this host. Browsers often open the API Railway URL by mistake;
 * this clarifies that the parent UI is served from a different origin.
 */
@RestController
class ApiRootController(private val appProperties: AppProperties) {

    @GetMapping("/")
    fun root(): ResponseEntity<Map<String, Any>> {
        val web = appProperties.auth.webBaseUrl.trim().removeSuffix("/")
        return ResponseEntity.ok(
            mapOf(
                "service" to "tamixa-api",
                "message" to "This host is the Tamixa REST API. Open parentWebAppUrl in your browser for the parent web app.",
                "parentWebAppUrl" to web,
                "health" to "${ApiVersion.V1}/health",
                "actuatorHealth" to "/actuator/health"
            )
        )
    }
}
