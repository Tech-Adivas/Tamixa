package com.tamixa.application.narration.impl

import com.tamixa.application.narration.NarrationPremiumVoiceValidator
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.domain.SubscriptionPlan
import org.springframework.stereotype.Service

/**
 * Server-side validation: premium/cloned voices require subscription.
 * - Default voice: always allowed.
 * - Cloned voice: allowed only for PREMIUM_MONTHLY, PREMIUM_YEARLY, FAMILY, VOICE_PREMIUM (monetization plan).
 * - Premium catalog voice (calm, etc.): requires subscription.voicePremium.
 */
@Service
class NarrationPremiumVoiceValidatorImpl(
    private val subscriptionService: SubscriptionService
) : NarrationPremiumVoiceValidator {

    private val plansWithVoiceCloning = setOf(
        SubscriptionPlan.PREMIUM_MONTHLY,
        SubscriptionPlan.PREMIUM_YEARLY,
        SubscriptionPlan.FAMILY,
        SubscriptionPlan.VOICE_PREMIUM
    )

    override fun canUseVoice(voiceProfile: String, parentId: Long?): Boolean {
        if (voiceProfile == "default") return true
        // Cloned voice: gate by plan (PREMIUM/FAMILY/VOICE_PREMIUM only; FREE/STARTER must upgrade)
        if (voiceProfile.startsWith("cloned:")) {
            if (parentId == null) return false
            val sub = subscriptionService.getOrCreateSubscription(parentId)
            return sub.plan in plansWithVoiceCloning && sub.isInGraceOrActive()
        }
        // Premium catalog voice (calm, etc.): need parentId and voicePremium flag
        if (parentId == null) return false
        val sub = subscriptionService.getOrCreateSubscription(parentId)
        return sub.allowsVoicePremium()
    }
}
