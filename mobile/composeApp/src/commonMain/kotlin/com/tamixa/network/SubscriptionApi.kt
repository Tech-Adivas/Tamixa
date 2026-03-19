package com.tamixa.network

import com.tamixa.domain.SubscriptionInfo
import com.tamixa.domain.UsageInfo
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*

class SubscriptionApi(private val client: HttpClient) {

    suspend fun getSubscription(): SubscriptionInfo? {
        val resp = client.get("${ApiConfig.API_VERSION}/subscription")
        val body = resp.body<SubscriptionResponse>()
        return SubscriptionInfo(
            isActive = body.status.lowercase() in listOf("active", "trial", "grace_period", "past_due"),
            planId = body.plan,
            expiresAt = body.currentPeriodEnd,
            trialEnd = body.trialEnd,
            cancelAtPeriodEnd = body.cancelAtPeriodEnd,
            maxChildren = body.maxChildren
        )
    }

    suspend fun getUsage(): UsageInfo? {
        val resp = client.get("${ApiConfig.API_VERSION}/subscription/usage")
        val body = resp.body<UsageResponse>()
        return UsageInfo(
            month = body.month,
            storiesUsed = body.storiesUsed,
            storiesLimit = body.storiesLimit,
            voiceUsed = body.voiceUsed,
            voiceLimit = body.voiceLimit
        )
    }

    suspend fun cancelSubscription(): Boolean = try {
        client.post("${ApiConfig.API_VERSION}/subscription/cancel")
        true
    } catch (_: Exception) {
        false
    }

    /** Validates a referral code. Returns discount info if valid and not expired, null otherwise. */
    suspend fun validateReferralCode(code: String): ReferralCodeValidateResponse? = try {
        val resp = client.get("${ApiConfig.API_VERSION}/subscription/referral-code/validate") {
            parameter("code", code.trim().uppercase())
        }
        resp.body<ReferralCodeValidateResponse>()
    } catch (_: Exception) {
        null
    }

    /** Creates checkout session for upgrade. Returns Stripe checkout URL or null. Pass referralCode to apply discount. */
    suspend fun createCheckoutSession(
        successUrl: String? = null,
        cancelUrl: String? = null,
        referralCode: String? = null
    ): String? = try {
        val body = UpgradeRequest(
            successUrl = successUrl,
            cancelUrl = cancelUrl,
            referralCode = referralCode?.takeIf { it.isNotBlank() }
        )
        val resp = client.post("${ApiConfig.API_VERSION}/subscription/upgrade") {
            setBody(body)
        }
        resp.body<UpgradeResponse>().checkoutUrl
    } catch (_: Exception) {
        null
    }
}

@kotlinx.serialization.Serializable
data class ReferralCodeValidateResponse(
    val valid: Boolean,
    val shortcode: String? = null,
    val shopName: String? = null,
    val offerPercent: Int? = null
)

@kotlinx.serialization.Serializable
private data class UpgradeRequest(
    val successUrl: String? = null,
    val cancelUrl: String? = null,
    val referralCode: String? = null
)

@kotlinx.serialization.Serializable
private data class UpgradeResponse(val checkoutUrl: String)

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
