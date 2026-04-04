package com.tamixa.network

import com.tamixa.domain.SubscriptionInfo
import com.tamixa.domain.UsageInfo
import com.tamixa.util.TamixaLog
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import kotlinx.coroutines.CancellationException

class SubscriptionApi(private val client: HttpClient) {

    suspend fun getSubscription(): SubscriptionInfo? {
        return try {
            val body = client.get("${ApiConfig.API_VERSION}/subscription").bodyIfSuccess<SubscriptionResponse>()
                ?: return null
            SubscriptionInfo(
                isActive = body.status.lowercase() in listOf("active", "trial", "grace_period", "past_due"),
                planId = body.plan,
                expiresAt = body.currentPeriodEnd,
                trialEnd = body.trialEnd,
                cancelAtPeriodEnd = body.cancelAtPeriodEnd,
                maxChildren = body.maxChildren
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TamixaLog.w("SubscriptionApi", "getSubscription failed", e)
            null
        }
    }

    suspend fun getUsage(): UsageInfo? {
        return try {
            val body = client.get("${ApiConfig.API_VERSION}/subscription/usage").bodyIfSuccess<UsageResponse>()
                ?: return null
            UsageInfo(
                month = body.month,
                storiesUsed = body.storiesUsed,
                storiesLimit = body.storiesLimit,
                voiceUsed = body.voiceUsed,
                voiceLimit = body.voiceLimit
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TamixaLog.w("SubscriptionApi", "getUsage failed", e)
            null
        }
    }

    suspend fun cancelSubscription(): Boolean = try {
        client.post("${ApiConfig.API_VERSION}/subscription/cancel")
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TamixaLog.w("SubscriptionApi", "cancelSubscription failed", e)
        false
    }

    /** Validates a referral code. Returns discount info if valid and not expired, null otherwise. */
    suspend fun validateReferralCode(code: String): ReferralCodeValidateResponse? = try {
        client.get("${ApiConfig.API_VERSION}/subscription/referral-code/validate") {
            parameter("code", code.trim().uppercase())
        }.bodyIfSuccess()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TamixaLog.w("SubscriptionApi", "validateReferralCode failed", e)
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
        client.post("${ApiConfig.API_VERSION}/subscription/upgrade") {
            setBody(body)
        }.bodyIfSuccess<UpgradeResponse>()?.checkoutUrl
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        TamixaLog.w("SubscriptionApi", "createCheckoutSession failed", e)
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
