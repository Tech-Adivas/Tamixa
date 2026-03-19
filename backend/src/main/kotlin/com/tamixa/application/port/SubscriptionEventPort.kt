package com.tamixa.application.port

import com.tamixa.domain.SubscriptionEvent

interface SubscriptionEventPort {

    fun append(event: SubscriptionEvent): SubscriptionEvent
}
