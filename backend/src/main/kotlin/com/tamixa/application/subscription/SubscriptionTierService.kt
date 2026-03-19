package com.tamixa.application.subscription

import com.tamixa.application.port.SubscriptionTierRepositoryPort
import com.tamixa.domain.SubscriptionPlan
import com.tamixa.domain.SubscriptionTier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubscriptionTierService(
    private val subscriptionTierRepository: SubscriptionTierRepositoryPort
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun getAllTiers(): List<SubscriptionTier> {
        return subscriptionTierRepository.findAll()
    }

    fun getTierByName(name: String): SubscriptionTier? {
        return subscriptionTierRepository.findByName(name)
    }

    fun getTierByPlan(plan: SubscriptionPlan): SubscriptionTier? {
        val tierName = when (plan) {
            SubscriptionPlan.FREE -> "FREE"
            SubscriptionPlan.STARTER -> "STARTER"
            SubscriptionPlan.PREMIUM_MONTHLY -> "PREMIUM"
            SubscriptionPlan.PREMIUM_YEARLY -> "PREMIUM"
            SubscriptionPlan.FAMILY -> "FAMILY"
            SubscriptionPlan.VOICE_PREMIUM -> "VOICE_PREMIUM"
        }
        return getTierByName(tierName)
    }

    @Transactional
    fun createDefaultTiers(): List<SubscriptionTier> {
        val existingTiers = subscriptionTierRepository.findAll()
        if (existingTiers.isNotEmpty()) {
            log.info("Tiers already exist, skipping creation")
            return existingTiers
        }

        val tiers = listOf(
            SubscriptionTier(
                id = 0,
                name = "FREE",
                priceMonthly = 0,
                priceYearly = 0,
                maxChildren = 1,
                maxVoices = 0,
                maxAvatarVideos = 0,
                maxSoundscapes = 0,
                allowsVoiceCloning = false,
                allowsAvatarVideo = false,
                allowsSoundscapes = false,
                allowsFamilySharing = false,
                analyticsEnabled = false,
                createdAt = java.time.Instant.now()
            ),
            SubscriptionTier(
                id = 0,
                name = "STARTER",
                priceMonthly = 9900,   // ₹99 (paise)
                priceYearly = 79900,  // ₹799
                maxChildren = 1,
                maxVoices = 0,
                maxAvatarVideos = 0,
                maxSoundscapes = 0,
                allowsVoiceCloning = false,
                allowsAvatarVideo = false,
                allowsSoundscapes = false,
                allowsFamilySharing = false,
                analyticsEnabled = false,
                createdAt = java.time.Instant.now()
            ),
            SubscriptionTier(
                id = 0,
                name = "PREMIUM",
                priceMonthly = 29900,   // ₹299 (paise)
                priceYearly = 239900,  // ₹2,399
                maxChildren = 3,
                maxVoices = 3,
                maxAvatarVideos = 10,
                maxSoundscapes = 20,
                allowsVoiceCloning = true,
                allowsAvatarVideo = true,
                allowsSoundscapes = true,
                allowsFamilySharing = false,
                analyticsEnabled = true,
                createdAt = java.time.Instant.now()
            ),
            SubscriptionTier(
                id = 0,
                name = "FAMILY",
                priceMonthly = 54900,   // ₹549 (paise)
                priceYearly = 439900,  // ₹4,399
                maxChildren = 10,
                maxVoices = 10,
                maxAvatarVideos = 50,
                maxSoundscapes = 100,
                allowsVoiceCloning = true,
                allowsAvatarVideo = true,
                allowsSoundscapes = true,
                allowsFamilySharing = true,
                analyticsEnabled = true,
                createdAt = java.time.Instant.now()
            )
        )

        return tiers.map { subscriptionTierRepository.save(it) }
    }
}
