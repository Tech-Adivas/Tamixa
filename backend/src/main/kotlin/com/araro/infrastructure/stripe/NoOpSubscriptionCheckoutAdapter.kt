package com.araro.infrastructure.stripe

import com.araro.application.port.SubscriptionCheckoutPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
@ConditionalOnProperty(name = ["app.subscription.stripe.enabled"], havingValue = "false", matchIfMissing = true)
class NoOpSubscriptionCheckoutAdapter : SubscriptionCheckoutPort {
    override fun createCheckoutSession(parentId: Long, successUrl: String, cancelUrl: String): String? = null
}
