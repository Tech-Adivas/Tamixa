package com.tamixa.application.notification

import com.tamixa.application.port.DeviceTokenRepositoryPort
import com.tamixa.application.port.PushNotificationPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for sending push notifications to parents.
 * Routes to appropriate adapter (FCM/APNs) based on device platform.
 *
 * Seasonal / “cultural clock” campaigns: copy and enablement live in [com.tamixa.infrastructure.config.AppProperties.seasonalHighlight];
 * scheduled fan-out is not implemented yet—parent apps can poll [com.tamixa.api.content.SeasonalHighlightController] for banner text.
 */
@Service
class PushNotificationService(
    private val deviceTokenRepository: DeviceTokenRepositoryPort,
    private val pushAdapters: List<PushNotificationPort>
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Send notification to all devices registered for a parent.
     */
    suspend fun sendToParent(
        parentId: Long,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Int {
        val tokens = deviceTokenRepository.findByParentId(parentId)
        if (tokens.isEmpty()) {
            logger.debug("No device tokens found for parent {}", parentId)
            return 0
        }

        var successCount = 0
        for (token in tokens) {
            val adapter = pushAdapters.firstOrNull() ?: continue
            val sent = try {
                adapter.sendNotification(token.token, title, body, data)
            } catch (e: Exception) {
                logger.error("Failed to send push to token {} (platform {}): {}", 
                    token.token.take(10), token.platform, e.message)
                false
            }
            if (sent) successCount++
        }

        logger.info("Sent push notification to {}/{} devices for parent {}", 
            successCount, tokens.size, parentId)
        return successCount
    }

    /**
     * Send notification to multiple parents (broadcast).
     */
    suspend fun sendToParents(
        parentIds: List<Long>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Int {
        var totalSent = 0
        for (parentId in parentIds) {
            totalSent += sendToParent(parentId, title, body, data)
        }
        return totalSent
    }
}
