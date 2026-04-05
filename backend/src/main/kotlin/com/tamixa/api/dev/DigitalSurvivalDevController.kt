package com.tamixa.api.dev

import com.tamixa.api.ApiVersion
import com.tamixa.application.dev.DigitalSurvivalDevE2eSeedService
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Dev-only Digital Survival E2E helpers: placeholder segment audio + one-shot DB preparation for Flyway seeds.
 * Security: routes under `/api/v1/dev/` are permitAll only when the `dev` Spring profile is active.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/dev/digital-survival")
@Profile("dev")
class DigitalSurvivalDevController(
    private val appProperties: AppProperties,
    private val digitalSurvivalDevE2eSeedService: DigitalSurvivalDevE2eSeedService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val placeholderResource = ClassPathResource("dev-assets/digital-survival-placeholder.mp3")

    @GetMapping("/placeholder.mp3", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun placeholderMp3(): ResponseEntity<ByteArray> {
        if (!placeholderResource.exists()) {
            log.error("Missing classpath dev-assets/digital-survival-placeholder.mp3")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
        val bytes = placeholderResource.inputStream.use { it.readBytes() }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("audio/mpeg"))
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(bytes)
    }

    @PostMapping("/prepare-e2e-seed")
    fun prepareE2eSeed(): ResponseEntity<Map<String, Any>> {
        if (!appProperties.digitalSurvivalDev.allowPrepareE2eSeedEndpoint) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                    mapOf(
                        "message" to
                            "Disabled. Set app.digital-survival-dev.allow-prepare-e2e-seed-endpoint=true " +
                            "(DIGITAL_SURVIVAL_DEV_PREPARE_E2E) or use application-dev defaults.",
                    ),
                )
        }
        val result = digitalSurvivalDevE2eSeedService.prepareE2eSeed()
        return ResponseEntity.ok(
            mapOf(
                "message" to "Digital Survival seed rows prepared for parent library API (dev only).",
                "storiesUpdated" to result.storiesUpdated,
                "translationsCreated" to result.translationsCreated,
                "narrationsUpserted" to result.narrationsUpserted,
                "next" to
                    "Set mobile language to a seeded locale (e.g. en). Open Library → Practice. " +
                    "Segment audio uses /api/v1/dev/digital-survival/placeholder.mp3 when graph rewrite is enabled.",
            ),
        )
    }
}
