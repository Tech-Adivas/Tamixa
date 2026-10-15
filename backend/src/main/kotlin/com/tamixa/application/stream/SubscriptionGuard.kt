package com.tamixa.application.stream

import com.tamixa.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.tamixa.application.narration.NarrationPremiumVoiceValidator
import com.tamixa.application.subscription.SubscriptionService
import com.tamixa.domain.subscription.UpgradeRequiredException
import org.springframework.stereotype.Component

/**
 * Server-side enforcement: premium catalog voices require subscription.voicePremium; cloned parent voices
 * ("cloned:{id}") require a plan that includes voice cloning.
 * Prevents client-side bypass of monetization gating.
 *
 * Architecture: Why server-enforced? Clients cannot be trusted—modified apps or
 * direct API calls could bypass client checks. All premium access must be
 * validated server-side before returning any stream URL.
 */
@Component
class SubscriptionGuard(
    private val voiceCatalog: NarrationVoiceCatalogRepositoryPort,
    private val subscriptionService: SubscriptionService,
    private val premiumVoiceValidator: NarrationPremiumVoiceValidator
) {

    /**
     * Throws UpgradeRequiredException if voice is premium and user lacks entitlement.
     * Call before returning stream URL for premium voices.
     */
    fun enforcePremiumVoiceAccess(voiceProfile: String, parentId: Long?, language: String = "en") {
        if (voiceProfile == "default") return
        // Cloned parent voice: needs a plan with voice cloning (narrating it may also create the paid provider clone).
        if (voiceProfile.startsWith("cloned:")) {
            if (parentId == null || !premiumVoiceValidator.canCloneVoice(parentId)) {
                throw UpgradeRequiredException("Upgrade required for your cloned voice")
            }
            return
        }
        val catalog = voiceCatalog.findByToneMode(voiceProfile)
            ?: return  // Unknown voice: allow (fail later if no audio)
        if (!catalog.isPremium) return
        if (parentId == null) throw UpgradeRequiredException("Premium voice requires subscription")
        val sub = subscriptionService.getOrCreateSubscription(parentId)
        if (!sub.allowsVoicePremium()) {
            throw UpgradeRequiredException("Upgrade required for premium voice: $voiceProfile")
        }
    }
}
