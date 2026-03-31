package com.tamixa.infrastructure.persistence

import com.tamixa.application.port.DevicePlatform
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "device_tokens",
    indexes = [
        Index(name = "idx_device_token_parent_id", columnList = "parent_id"),
        Index(name = "idx_device_token_token", columnList = "token", unique = true)
    ]
)
data class DeviceTokenEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "parent_id", nullable = false)
    val parentId: Long,

    @Column(name = "token", nullable = false, unique = true, length = 512)
    val token: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    val platform: DevicePlatform,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_used_at", nullable = false)
    val lastUsedAt: Instant = Instant.now()
)
