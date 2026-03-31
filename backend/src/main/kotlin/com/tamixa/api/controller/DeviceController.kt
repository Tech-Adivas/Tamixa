package com.tamixa.api.controller

import com.tamixa.application.port.DevicePlatform
import com.tamixa.application.port.DeviceToken
import com.tamixa.application.port.DeviceTokenRepositoryPort
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Device", description = "Device token registration for push notifications")
class DeviceController(
    private val deviceTokenRepository: DeviceTokenRepositoryPort,
    private val parentRepository: com.tamixa.application.port.ParentRepositoryPort
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/push-token")
    @Operation(summary = "Register device push token", description = "Register FCM (Android) or APNs (iOS) token for push notifications")
    fun registerPushToken(
        @RequestBody request: RegisterPushTokenRequest
    ): ResponseEntity<Map<String, Any>> {
        val email = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))

        val parent = parentRepository.findByEmail(email)
            ?: return ResponseEntity.status(404).body(mapOf("error" to "Parent not found"))

        // Check if token already exists
        val existing = deviceTokenRepository.findByToken(request.token)
        if (existing != null) {
            // Update last_used_at
            val updated = existing.copy(lastUsedAt = Instant.now())
            deviceTokenRepository.save(updated)
            logger.debug("Updated existing device token for parent {}", parent.id)
            return ResponseEntity.ok(mapOf("status" to "updated"))
        }

        // Save new token
        val deviceToken = DeviceToken(
            parentId = parent.id,
            token = request.token,
            platform = request.platform
        )
        deviceTokenRepository.save(deviceToken)
        logger.info("Registered new {} push token for parent {}", request.platform, parent.id)

        return ResponseEntity.ok(mapOf("status" to "registered"))
    }

    @DeleteMapping("/push-token")
    @Operation(summary = "Unregister device push token", description = "Remove device token (e.g., on logout)")
    fun unregisterPushToken(
        @RequestBody request: UnregisterPushTokenRequest
    ): ResponseEntity<Map<String, Any>> {
        val email = org.springframework.security.core.context.SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))

        val parent = parentRepository.findByEmail(email)
            ?: return ResponseEntity.status(404).body(mapOf("error" to "Parent not found"))

        deviceTokenRepository.deleteByToken(request.token)
        logger.info("Unregistered push token for parent {}", parent.id)

        return ResponseEntity.ok(mapOf("status" to "unregistered"))
    }
}

data class RegisterPushTokenRequest(
    val token: String,
    val platform: DevicePlatform
)

data class UnregisterPushTokenRequest(
    val token: String
)
