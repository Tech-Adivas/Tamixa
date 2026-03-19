package com.tamixa.api.admin.dto

data class AiMetricsDto(
    val storyGenerationsTotal: Long,
    val cacheHits: Long,
    val cacheMisses: Long,
    val voiceProcessingCount: Long,
    val openaiTokensUsed: Long?,
    /** Per-API usage breakdown for admin UI. */
    val apiBreakdown: List<ApiUsageDto> = emptyList(),
    /** Per-story AI usage (tokens, avatar count, cost in ₹). */
    val storyBreakdown: List<StoryAiUsageDto> = emptyList()
)
