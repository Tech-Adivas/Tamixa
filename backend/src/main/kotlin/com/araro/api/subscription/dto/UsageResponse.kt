package com.araro.api.subscription.dto

data class UsageResponse(
    val month: String,
    val storiesUsed: Int,
    val storiesLimit: Int?,
    val voiceUsed: Int,
    val voiceLimit: Int
)
