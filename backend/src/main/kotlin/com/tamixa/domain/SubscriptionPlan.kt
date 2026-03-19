package com.tamixa.domain

/**
 * Subscription plan identifiers. Entitlements (e.g. story limit, voice) are derived from plan.
 */
enum class SubscriptionPlan {
    FREE,           // 5 stories/month
    STARTER,        // 15 stories/month, no voice/avatar (India INR)
    PREMIUM_MONTHLY,
    PREMIUM_YEARLY,
    FAMILY,         // multiple children
    VOICE_PREMIUM   // add-on voice training
}
