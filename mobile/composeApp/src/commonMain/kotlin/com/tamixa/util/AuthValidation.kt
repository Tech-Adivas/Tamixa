package com.tamixa.util

/**
 * Client-side validation for auth flows (audit recommendation).
 * Keeps validation rules in one place and avoids unnecessary API calls.
 */
object AuthValidation {
    // RFC 5322-inspired: local@domain.tld — rejects "a@b", requires dot in domain
    private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

    /** Validates email format: must match local@domain.tld pattern. */
    fun isEmailValid(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && EMAIL_REGEX.matches(trimmed)
    }

    /** OTP/code: digits only, length in [4, 8] (typical for email OTP). */
    fun isOtpCodeValid(code: String): Boolean {
        val trimmed = code.trim()
        return trimmed.length in 4..8 && trimmed.all { it.isDigit() }
    }
}
