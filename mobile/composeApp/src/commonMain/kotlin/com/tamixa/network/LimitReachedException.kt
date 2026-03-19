package com.tamixa.network

/**
 * Thrown when story generation fails with 402 (free/starter plan limit reached).
 * UI can show upgrade nudge and navigate to Subscription.
 */
class LimitReachedException(
    message: String = "Story limit reached. Upgrade for more.",
    val recommendedPlan: String = "PREMIUM"
) : Exception(message)
