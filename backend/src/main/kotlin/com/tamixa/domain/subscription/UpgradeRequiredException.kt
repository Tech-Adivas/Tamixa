package com.tamixa.domain.subscription

/**
 * Thrown when user attempts to access premium content (e.g. premium voice)
 * without an active premium subscription.
 *
 * Server-enforced gating: clients cannot bypass premium checks.
 */
class UpgradeRequiredException(message: String = "Premium subscription required") : RuntimeException(message)
