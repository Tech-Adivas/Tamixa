package com.tamixa.infrastructure.persistence

import com.tamixa.domain.Role
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "parents")
class ParentEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    val passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var role: Role,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(length = 20, unique = true)
    var phone: String? = null,

    @Column(length = 100)
    var nickname: String? = null,

    @Column(name = "display_name", length = 100)
    var displayName: String? = null,

    @Column(name = "suspended_at")
    var suspendedAt: Instant? = null,

    @Column(name = "story_art_personalization_opt_in", nullable = false)
    var storyArtPersonalizationOptIn: Boolean = false
)
