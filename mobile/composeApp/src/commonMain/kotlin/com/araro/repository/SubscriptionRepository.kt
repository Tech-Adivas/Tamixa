package com.araro.repository

import com.araro.domain.SubscriptionInfo
import com.araro.domain.UsageInfo
import com.araro.network.SubscriptionApi

class SubscriptionRepository(private val api: SubscriptionApi) {

    suspend fun getSubscription(): SubscriptionInfo? = api.getSubscription()

    suspend fun getUsage(): UsageInfo? = api.getUsage()

    suspend fun cancelSubscription(): Boolean = api.cancelSubscription()
}
