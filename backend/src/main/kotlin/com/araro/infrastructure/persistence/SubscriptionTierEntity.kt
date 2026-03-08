package com.araro.infrastructure.persistence

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "subscription_tiers")
data class SubscriptionTierEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 100)
    val name: String,

    @Column(nullable = false)
    val priceMonthly: Long,

    @Column(nullable = false)
    val priceYearly: Long,

    @Column(nullable = false)
    val maxChildren: Int,

    @Column(nullable = false)
    val maxVoices: Int,

    @Column(nullable = false)
    val maxAvatarVideos: Int,

    @Column(nullable = false)
    val maxSoundscapes: Int,

    @Column(nullable = false)
    val allowsVoiceCloning: Boolean,

    @Column(nullable = false)
    val allowsAvatarVideo: Boolean,

    @Column(nullable = false)
    val allowsSoundscapes: Boolean,

    @Column(nullable = false)
    val allowsFamilySharing: Boolean,

    @Column(nullable = false)
    val analyticsEnabled: Boolean,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
