package com.tamixa.util

/**
 * Client-side validation for auth flows (audit recommendation).
 * Keeps validation rules in one place and avoids unnecessary API calls.
 */
object AuthValidation {
    /** Simple email format: non-empty, contains @, has domain part. */
    fun isEmailValid(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return false
        val at = trimmed.indexOf('@')
        return at in 1..(trimmed.length - 2)
    }

    /** OTP/code: digits only, length in [4, 8] (typical for email OTP). */
    fun isOtpCodeValid(code: String): Boolean {
        val trimmed = code.trim()
        return trimmed.length in 4..8 && trimmed.all { it.isDigit() }
    }
}
