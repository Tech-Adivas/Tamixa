package com.tamixa.api.admin.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

data class RecordDoraDeploymentRequest(
    @field:NotBlank(message = "serviceName is required")
    @field:Size(max = 80)
    val serviceName: String,

    @field:NotBlank(message = "environment is required")
    @field:Size(max = 40)
    val environment: String,

    @field:NotBlank(message = "status is required")
    @field:Pattern(regexp = "SUCCESS|FAILED", message = "status must be SUCCESS or FAILED")
    val status: String,

    @field:Size(max = 120)
    val changeId: String? = null,

    val changeStartedAt: Instant? = null,
    val deployedAt: Instant? = null,

    @field:Size(max = 500)
    val summary: String? = null
)
