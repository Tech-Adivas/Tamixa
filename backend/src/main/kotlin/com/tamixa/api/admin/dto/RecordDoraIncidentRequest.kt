package com.tamixa.api.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

data class RecordDoraIncidentRequest(
    @field:NotBlank(message = "serviceName is required")
    @field:Size(max = 80)
    val serviceName: String,

    @field:NotBlank(message = "environment is required")
    @field:Size(max = 40)
    val environment: String,

    @field:NotBlank(message = "status is required")
    @field:Pattern(regexp = "OPEN|RESOLVED", message = "status must be OPEN or RESOLVED")
    val status: String,

    @field:NotNull(message = "incidentStartedAt is required")
    val incidentStartedAt: Instant,

    val restoredAt: Instant? = null,
    val causedByChange: Boolean = true,

    @field:Size(max = 500)
    val summary: String? = null
)
