package com.tamixa.api.auth.dto

data class OtpSendResponse(
    val sent: Boolean,
    /** Dev-only: OTP code when DEV_OTP_CODE is used. Never exposed in prod. */
    val code: String? = null
)
