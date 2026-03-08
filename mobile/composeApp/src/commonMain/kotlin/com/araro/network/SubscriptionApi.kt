package com.araro.network

import com.araro.domain.SubscriptionInfo
import com.araro.domain.UsageInfo
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*

class SubscriptionApi(private val client: HttpClient) {

    suspend fun getSubscription(): SubscriptionInfo? = try {
        val resp = client.get("${ApiConfig.API_VERSION}/subscription")
        val body = resp.body<SubscriptionResponse>()
        SubscriptionInfo(
            isActive = body.status.lowercase() in listOf("active", "trial", "grace_period", "past_due"),
            planId = body.plan,
            expiresAt = body.currentPeriodEnd,
            trialEnd = body.trialEnd,
            cancelAtPeriodEnd = body.cancelAtPeriodEnd,
            maxChildren = body.maxChildren
        )
    } catch (_: Exception) {
        null
    }

    suspend fun getUsage(): UsageInfo? = try {
        val resp = client.get("${ApiConfig.API_VERSION}/subscription/usage")
        val body = resp.body<UsageResponse>()
        UsageInfo(
            month = body.month,
            storiesUsed = body.storiesUsed,
            storiesLimit = body.storiesLimit,
            voiceUsed = body.voiceUsed,
            voiceLimit = body.voiceLimit
        )
    } catch (_: Exception) {
        null
    }

    suspend fun cancelSubscription(): Boolean = try {
        client.post("${ApiConfig.API_VERSION}/subscription/cancel")
        true
    } catch (_: Exception) {
        false
    }
}

@kotlinx.serialization.Serializable
private data class UsageResponse(
    val month: String,
    val storiesUsed: Int,
    val storiesLimit: Int?,
    val voiceUsed: Int,
    val voiceLimit: Int
)

@kotlinx.serialization.Serializable
private data class SubscriptionResponse(
    val plan: String,
    val status: String,
    val provider: String?,
    val currentPeriodEnd: String?,
    val trialEnd: String?,
    val cancelAtPeriodEnd: Boolean,
    val maxChildren: Int,
    val voicePremium: Boolean,
    val isEntitledToUnlimitedStories: Boolean
)
