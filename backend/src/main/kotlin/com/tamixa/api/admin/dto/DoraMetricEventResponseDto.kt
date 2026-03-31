package com.tamixa.api.admin.dto

import java.time.Instant

data class DoraMetricEventResponseDto(
    val id: Long,
    val eventType: String,
    val serviceName: String,
    val environment: String,
    val createdAt: Instant
)
