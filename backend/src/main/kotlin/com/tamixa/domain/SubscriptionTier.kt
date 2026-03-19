package com.tamixa.domain

import java.time.Instant

/**
 * Subscription tier with specific entitlements.
 * FREE: Basic features, limited stories
 * PREMIUM: Unlimited stories, voice cloning, avatar videos
 * ENTERPRISE: All premium features + family sharing, advanced analytics
 */
data class SubscriptionTier(
    val id: Long,
    val name: String,
    val priceMonthly: Long,
    val priceYearly: Long,
    val maxChildren: Int,
    val maxVoices: Int,
    val maxAvatarVideos: Int,
    val maxSoundscapes: Int,
    val allowsVoiceCloning: Boolean,
    val allowsAvatarVideo: Boolean,
    val allowsSoundscapes: Boolean,
    val allowsFamilySharing: Boolean,
    val analyticsEnabled: Boolean,
    val createdAt: Instant
)
