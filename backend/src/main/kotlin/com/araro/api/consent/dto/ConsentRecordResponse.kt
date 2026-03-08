package com.araro.api.consent.dto

data class ConsentRecordResponse(
    val consentType: String,
    val version: Int,
    val grantedAt: String
)
