package com.tamixa.repository

import com.tamixa.domain.SubscriptionInfo
import com.tamixa.domain.UsageInfo
import com.tamixa.network.SubscriptionApi

class SubscriptionRepository(private val api: SubscriptionApi) {

    suspend fun getSubscription(): SubscriptionInfo? = api.getSubscription()

    suspend fun getUsage(): UsageInfo? = api.getUsage()

    suspend fun cancelSubscription(): Boolean = api.cancelSubscription()

    suspend fun validateReferralCode(code: String): ReferralCodeInfo? {
        val resp = api.validateReferralCode(code) ?: return null
        if (!resp.valid) return null
        return ReferralCodeInfo(
            shortcode = resp.shortcode ?: code.trim().uppercase(),
            shopName = resp.shopName ?: "",
            offerPercent = resp.offerPercent ?: 0
        )
    }

    suspend fun createCheckoutSession(successUrl: String?, cancelUrl: String?, referralCode: String?): String? =
        api.createCheckoutSession(successUrl, cancelUrl, referralCode)
}

data class ReferralCodeInfo(
    val shortcode: String,
    val shopName: String,
    val offerPercent: Int
)
