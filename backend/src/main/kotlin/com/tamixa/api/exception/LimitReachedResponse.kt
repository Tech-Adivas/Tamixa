package com.tamixa.api.exception

/**
 * Structured response when free usage limit is reached.
 * Mobile UI can detect status=LIMIT_REACHED and show upgrade nudge.
 * Do NOT throw generic errors—use this for upgrade conversion.
 */
data class LimitReachedResponse(
    val status: String = "LIMIT_REACHED",
    val remaining: Int = 0,
    val upgradeRequired: Boolean = true,
    val recommendedPlan: String = "PREMIUM"
)
