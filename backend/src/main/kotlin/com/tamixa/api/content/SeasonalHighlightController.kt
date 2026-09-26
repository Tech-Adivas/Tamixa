package com.tamixa.api.content

import com.tamixa.api.ApiVersion
import com.tamixa.api.content.dto.SeasonalHighlightResponse
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Read-only seasonal highlight for parent clients (banner stub). Push scheduling not implemented yet.
 */
@RestController
@RequestMapping("${ApiVersion.V1}/seasonal")
@PreAuthorize("hasRole('PARENT')")
class SeasonalHighlightController(
    private val appProperties: AppProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/highlight")
    fun highlight(): SeasonalHighlightResponse {
        val s = appProperties.seasonalHighlight
        log.debug("Seasonal highlight campaign={} enabled={}", s.campaignKey, s.enabled)
        return SeasonalHighlightResponse(
            enabled = s.enabled,
            campaignKey = s.campaignKey,
            titleEn = s.titleEn,
            bodyEn = s.bodyEn,
            pushDispatchStub = s.pushDispatchStub,
        )
    }
}
