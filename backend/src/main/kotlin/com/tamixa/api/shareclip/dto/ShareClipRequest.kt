package com.tamixa.api.shareclip.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class ShareClipRequest(
    @field:Min(0) val startSeconds: Int = 0,
    @field:Min(15) @field:Max(60) val durationSeconds: Int = 30,
    val format: String? = "9:16"
)
