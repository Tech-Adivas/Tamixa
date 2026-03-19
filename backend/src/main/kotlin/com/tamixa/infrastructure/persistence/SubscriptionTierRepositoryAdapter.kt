package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.SubscriptionTierRepositoryPort
import com.tamixa.domain.SubscriptionTier
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SubscriptionTierRepositoryAdapter(
    private val subscriptionTierJpaRepository: SubscriptionTierJpaRepository
) : SubscriptionTierRepositoryPort {

    @Transactional
    override fun save(tier: SubscriptionTier): SubscriptionTier {
        val entity = SubscriptionTierEntity(
            id = tier.id,
            name = tier.name,
            priceMonthly = tier.priceMonthly,
            priceYearly = tier.priceYearly,
            maxChildren = tier.maxChildren,
            maxVoices = tier.maxVoices,
            maxAvatarVideos = tier.maxAvatarVideos,
            maxSoundscapes = tier.maxSoundscapes,
            allowsVoiceCloning = tier.allowsVoiceCloning,
            allowsAvatarVideo = tier.allowsAvatarVideo,
            allowsSoundscapes = tier.allowsSoundscapes,
            allowsFamilySharing = tier.allowsFamilySharing,
            analyticsEnabled = tier.analyticsEnabled,
            createdAt = tier.createdAt
        )
        val saved = subscriptionTierJpaRepository.save(entity)
        return SubscriptionTier(
            id = saved.id,
            name = saved.name,
            priceMonthly = saved.priceMonthly,
            priceYearly = saved.priceYearly,
            maxChildren = saved.maxChildren,
            maxVoices = saved.maxVoices,
            maxAvatarVideos = saved.maxAvatarVideos,
            maxSoundscapes = saved.maxSoundscapes,
            allowsVoiceCloning = saved.allowsVoiceCloning,
            allowsAvatarVideo = saved.allowsAvatarVideo,
            allowsSoundscapes = saved.allowsSoundscapes,
            allowsFamilySharing = saved.allowsFamilySharing,
            analyticsEnabled = saved.analyticsEnabled,
            createdAt = saved.createdAt
        )
    }

    override fun findById(id: Long): SubscriptionTier? {
        return subscriptionTierJpaRepository.findById(id).map {
            SubscriptionTier(
                id = it.id,
                name = it.name,
                priceMonthly = it.priceMonthly,
                priceYearly = it.priceYearly,
                maxChildren = it.maxChildren,
                maxVoices = it.maxVoices,
                maxAvatarVideos = it.maxAvatarVideos,
                maxSoundscapes = it.maxSoundscapes,
                allowsVoiceCloning = it.allowsVoiceCloning,
                allowsAvatarVideo = it.allowsAvatarVideo,
                allowsSoundscapes = it.allowsSoundscapes,
                allowsFamilySharing = it.allowsFamilySharing,
                analyticsEnabled = it.analyticsEnabled,
                createdAt = it.createdAt
            )
        }.orElse(null)
    }

    override fun findByName(name: String): SubscriptionTier? {
        return subscriptionTierJpaRepository.findByName(name)?.let {
            SubscriptionTier(
                id = it.id,
                name = it.name,
                priceMonthly = it.priceMonthly,
                priceYearly = it.priceYearly,
                maxChildren = it.maxChildren,
                maxVoices = it.maxVoices,
                maxAvatarVideos = it.maxAvatarVideos,
                maxSoundscapes = it.maxSoundscapes,
                allowsVoiceCloning = it.allowsVoiceCloning,
                allowsAvatarVideo = it.allowsAvatarVideo,
                allowsSoundscapes = it.allowsSoundscapes,
                allowsFamilySharing = it.allowsFamilySharing,
                analyticsEnabled = it.analyticsEnabled,
                createdAt = it.createdAt
            )
        }
    }

    override fun findAll(): List<SubscriptionTier> {
        return subscriptionTierJpaRepository.findAll().map {
            SubscriptionTier(
                id = it.id,
                name = it.name,
                priceMonthly = it.priceMonthly,
                priceYearly = it.priceYearly,
                maxChildren = it.maxChildren,
                maxVoices = it.maxVoices,
                maxAvatarVideos = it.maxAvatarVideos,
                maxSoundscapes = it.maxSoundscapes,
                allowsVoiceCloning = it.allowsVoiceCloning,
                allowsAvatarVideo = it.allowsAvatarVideo,
                allowsSoundscapes = it.allowsSoundscapes,
                allowsFamilySharing = it.allowsFamilySharing,
                analyticsEnabled = it.analyticsEnabled,
                createdAt = it.createdAt
            )
        }
    }
}
