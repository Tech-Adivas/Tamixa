package com.tamixa.api.admin.dto

/**
 * Per-API usage for admin AI metrics breakdown.
 */
data class ApiUsageDto(
    val api: String,
    val displayName: String,
    val requests: Long,
    val tokensOrCharacters: Long? = null,
    val costEstimateUsd: Double? = null
)
