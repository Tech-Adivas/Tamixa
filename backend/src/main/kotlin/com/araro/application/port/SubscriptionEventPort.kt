package com.araro.application.port

import com.araro.domain.SubscriptionEvent

interface SubscriptionEventPort {

    fun append(event: SubscriptionEvent): SubscriptionEvent
}
