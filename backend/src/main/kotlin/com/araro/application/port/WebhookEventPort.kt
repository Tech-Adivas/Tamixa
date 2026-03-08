package com.araro.application.port

/**
 * Records every webhook event for audit and replay prevention.
 * Duplicate event_id is rejected at persistence (unique constraint); idempotency check must run before recording.
 * Raw payload is encrypted at rest when webhook encryption key is configured.
 */
interface WebhookEventPort {

    /**
     * Records a webhook event. rawPayload is encrypted before storage when key is set; otherwise not stored.
     * Call only after idempotency check (alreadyProcessed = false) to avoid unique violation.
     */
    fun record(
        provider: String,
        eventId: String,
        eventType: String,
        status: WebhookEventStatus,
        rawPayload: String?
    )
}

enum class WebhookEventStatus {
    PROCESSED,
    FAILED
}
