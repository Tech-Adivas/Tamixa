package com.tamixa.api.shareclip.dto

data class ShareClipQuotaResponse(
    val remaining: Int,
    val limit: Int,
    val entitled: Boolean
)
