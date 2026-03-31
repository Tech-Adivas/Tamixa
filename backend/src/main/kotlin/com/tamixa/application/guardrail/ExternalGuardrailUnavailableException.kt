package com.tamixa.application.guardrail

/**
 * Remote guardrails HTTP service unreachable or errored; mapped to HTTP 503 when fail-open is false.
 */
class ExternalGuardrailUnavailableException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)
