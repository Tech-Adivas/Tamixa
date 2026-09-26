package com.tamixa.api.content.dto

/**
 * Parent-app “cultural clock” / seasonal banner payload. Copy is English-only until i18n pipeline extends this DTO.
 */
data class SeasonalHighlightResponse(
    val enabled: Boolean,
    val campaignKey: String,
    val titleEn: String,
    val bodyEn: String,
    /** Reserved for future FCM batch jobs; always false until implemented. */
    val pushDispatchStub: Boolean,
)
