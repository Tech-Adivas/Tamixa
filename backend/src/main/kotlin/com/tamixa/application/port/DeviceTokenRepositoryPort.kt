package com.tamixa.application.port

import java.time.Instant

/**
 * Repository for storing device push notification tokens.
 */
interface DeviceTokenRepositoryPort {
    
    fun save(deviceToken: DeviceToken): DeviceToken
    
    fun findByParentId(parentId: Long): List<DeviceToken>
    
    fun findByToken(token: String): DeviceToken?
    
    fun deleteByToken(token: String)
    
    fun deleteByParentId(parentId: Long)
}

data class DeviceToken(
    val id: Long = 0,
    val parentId: Long,
    val token: String,
    val platform: DevicePlatform,
    val createdAt: Instant = Instant.now(),
    val lastUsedAt: Instant = Instant.now()
)

enum class DevicePlatform {
    ANDROID,
    IOS
}
