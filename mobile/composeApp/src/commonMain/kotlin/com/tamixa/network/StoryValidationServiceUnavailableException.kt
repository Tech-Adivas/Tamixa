package com.tamixa.network

/**
 * Structured-output validation sidecar returned HTTP 503 (e.g. guardrails service down, fail-open disabled).
 * [message] may include the API error text; safe to show to the parent.
 */
class StoryValidationServiceUnavailableException(
    message: String
) : Exception(message)
