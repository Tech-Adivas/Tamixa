package com.araro.api.admin.dto

data class AiMetricsDto(
    val storyGenerationsTotal: Long,
    val cacheHits: Long,
    val cacheMisses: Long,
    val voiceProcessingCount: Long,
    val openaiTokensUsed: Long?
)
