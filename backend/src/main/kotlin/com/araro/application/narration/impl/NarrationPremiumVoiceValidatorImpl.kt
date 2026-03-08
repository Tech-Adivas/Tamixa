package com.araro.application.narration.impl

import com.araro.application.narration.NarrationPremiumVoiceValidator
import com.araro.application.subscription.SubscriptionService
import org.springframework.stereotype.Service

/**
 * Server-side validation: premium voices require subscription.voicePremium.
 * Kafka pipeline (parentId=null) only allows "default"; API requests with parentId validate subscription.
 */
@Service
class NarrationPremiumVoiceValidatorImpl(
    private val subscriptionService: SubscriptionService
) : NarrationPremiumVoiceValidator {

    override fun canUseVoice(voiceProfile: String, parentId: Long?): Boolean {
        if (voiceProfile == "default") return true
        // Cloned voice (parent's own): allow when parentId present (ownership verified at synthesis)
        if (voiceProfile.startsWith("cloned:")) return parentId != null
        // Premium voice (calm, etc.): need parentId and subscription check
        if (parentId == null) return false
        val sub = subscriptionService.getOrCreateSubscription(parentId)
        return sub.allowsVoicePremium()
    }
}
