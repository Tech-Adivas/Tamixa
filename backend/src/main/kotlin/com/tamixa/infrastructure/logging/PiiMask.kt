package com.tamixa.infrastructure.logging

/**
 * PII masking for logs. Per security rules: never log passwords, tokens, API keys, or PII.
 * Child names, emails, phones must be masked or omitted.
 *
 * ## Important: One-Way Only – No Original Values
 *
 * PiiMask does NOT store or retain original values. It is a one-way transformation:
 * you pass the original value in, you get a masked string for logs. There is no
 * way to "unmask" or recover the original from the masked output.
 *
 * ## How to Correlate Logs with Real Users (Support/Debug)
 *
 * 1. **Use IDs in logs** – parentId, storyId, childId, traceId (not masked)
 * 2. **traceId** – Present in MDC and error responses; correlate across requests
 * 3. **Look up in database** – Use parentId → query parent table for email/phone
 * 4. **Admin tools** – Use IDs to look up records in admin UI or DB
 *
 * Example: Audit log shows "login_attempt email=ab***@example.com success=false".
 * To find the real user: search logs for traceId, get parentId from the same request,
 * then `SELECT * FROM parent WHERE id = ?` in your DB.
 */
object PiiMask {

    /**
     * Mask email: ab***@domain.com
     */
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank()) return "***"
        if (email.length < 5) return "***"
        val at = email.indexOf('@')
        if (at <= 0) return "***"
        return email.take(2) + "***" + email.drop(at)
    }

    /**
     * Mask phone: ***1234 (last 4 digits only)
     */
    fun maskPhone(phone: String?): String {
        if (phone.isNullOrBlank()) return "***"
        return if (phone.length >= 4) "***${phone.takeLast(4)}" else "***"
    }

    /**
     * Mask token/secret: ***last4
     */
    fun maskToken(token: String?): String {
        if (token.isNullOrBlank()) return "***"
        return if (token.length >= 4) "***${token.takeLast(4)}" else "***"
    }

    /**
     * Mask child name: first 2 chars + *** (e.g. "Ra***" for "Ravi").
     * Per children's app rules: no full child names in logs.
     */
    fun maskChildName(name: String?): String {
        if (name.isNullOrBlank()) return "***"
        return if (name.length <= 2) "***" else name.take(2) + "***"
    }

    /**
     * Redact entirely – use for OTP, password, API key, etc.
     */
    fun redact(): String = "***"

    /**
     * Sanitize for logging: null/blank → "***", else return as-is.
     * Use only for non-PII values (e.g. IDs, status codes).
     */
    fun sanitize(value: Any?): String =
        when {
            value == null -> "null"
            value is String && value.isBlank() -> "***"
            else -> value.toString()
        }
}
