package com.araro.domain

/**
 * Subscription plan identifiers. Entitlements (e.g. story limit, voice) are derived from plan.
 */
enum class SubscriptionPlan {
    FREE,           // 5 stories/month
    PREMIUM_MONTHLY,
    PREMIUM_YEARLY,
    FAMILY,         // multiple children
    VOICE_PREMIUM   // add-on voice training
}
