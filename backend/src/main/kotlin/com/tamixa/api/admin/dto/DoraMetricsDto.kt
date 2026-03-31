package com.tamixa.api.admin.dto

data class DoraMetricsDto(
    val windowDays: Int,
    val serviceName: String?,
    val environment: String?,
    val successfulDeployments: Long,
    val failedDeployments: Long,
    val deploymentFrequencyPerDay: Double,
    val changeFailureRatePercent: Double,
    val leadTimeMinutesP50: Long?,
    val leadTimeMinutesP95: Long?,
    val meanTimeToRestoreMinutes: Long?,
    val openIncidents: Long,
    val resolvedIncidents: Long
)
