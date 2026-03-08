package com.araro.application.stream

import com.araro.application.port.narration.NarrationVoiceCatalogRepositoryPort
import com.araro.application.subscription.SubscriptionService
import com.araro.domain.subscription.UpgradeRequiredException
import org.springframework.stereotype.Component

/**
 * Server-side enforcement: premium voice access requires subscription.voicePremium.
 * Prevents client-side bypass of monetization gating.
 *
 * Architecture: Why server-enforced? Clients cannot be trusted—modified apps or
 * direct API calls could bypass client checks. All premium access must be
 * validated server-side before returning any stream URL.
 */
@Component
class SubscriptionGuard(
    private val voiceCatalog: NarrationVoiceCatalogRepositoryPort,
    private val subscriptionService: SubscriptionService
) {

    /**
     * Throws UpgradeRequiredException if voice is premium and user lacks entitlement.
     * Call before returning stream URL for premium voices.
     */
    fun enforcePremiumVoiceAccess(voiceProfile: String, parentId: Long?, language: String = "en") {
        if (voiceProfile == "default") return
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
