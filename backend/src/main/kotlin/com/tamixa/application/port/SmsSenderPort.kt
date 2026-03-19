package com.tamixa.application.port

/**
 * Sends SMS (OTP codes for phone login).
 * Implementations: LoggingSmsSender (dev), TwilioSmsSender (prod when TWILIO_* configured).
 */
interface SmsSenderPort {
    /** Sends OTP to phone. Returns true if sent successfully. */
    fun sendOtp(toPhone: String, code: String): Boolean
}
