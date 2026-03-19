package com.tamixa.domain

/**
 * Explicit subscription lifecycle states. All transitions must be validated via SubscriptionStateMachine.
 * Financial safety: never trust client-reported status; always resolve server-side.
 *
 * FREE          -> TRIAL (start trial) | ACTIVE (direct upgrade)
 * TRIAL         -> ACTIVE | EXPIRED | CANCELED
 * ACTIVE        -> PAST_DUE | CANCELED | EXPIRED
 * PAST_DUE      -> GRACE_PERIOD | ACTIVE | EXPIRED
 * GRACE_PERIOD  -> ACTIVE | EXPIRED (after grace window)
 * CANCELED      -> EXPIRED (at period end)
 * EXPIRED       -> (terminal)
 * PAYMENT_FAILED-> PAST_DUE | ACTIVE (recovered)
 */
enum class SubscriptionStatus {
    FREE,           // no paid subscription; free tier limits apply
    TRIAL,          // in trial period (e.g. 7-day)
    ACTIVE,         // paid and current
    PAST_DUE,       // payment failed; provider retrying
    GRACE_PERIOD,   // post past_due; allow N-day grace then expire
    CANCELED,       // user canceled; access until period end
    EXPIRED,        // no access; terminal
    PAYMENT_FAILED  // payment failed (may transition to PAST_DUE)
}
