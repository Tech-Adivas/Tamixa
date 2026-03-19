package com.tamixa.api.shareclip.dto

data class ShareClipResponse(
    val clipId: Long,
    val status: String,
    val downloadUrl: String? = null,
    val errorMessage: String? = null
)
