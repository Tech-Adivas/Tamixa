package com.tamixa.domain

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionInfo(
    val isActive: Boolean,
    val planId: String? = null,
    val expiresAt: String? = null,
    val trialEnd: String? = null,
    val cancelAtPeriodEnd: Boolean = false,
    val maxChildren: Int = 1
)

@Serializable
data class UsageInfo(
    val month: String,
    val storiesUsed: Int,
    val storiesLimit: Int?,
    val voiceUsed: Int,
    val voiceLimit: Int
)
