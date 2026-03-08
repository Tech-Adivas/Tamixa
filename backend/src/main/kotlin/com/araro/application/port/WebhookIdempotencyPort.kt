package com.araro.application.port

/**
 * Ensures each webhook event (provider + eventId) is processed at most once.
 */
interface WebhookIdempotencyPort {

    fun alreadyProcessed(provider: String, eventId: String): Boolean

    fun markProcessed(provider: String, eventId: String)
}
