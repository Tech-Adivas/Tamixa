package com.tamixa.application.port

/**
 * Port for sending push notifications to mobile devices.
 * Implementations: FCMPushNotificationAdapter (Android), APNsPushNotificationAdapter (iOS).
 */
interface PushNotificationPort {
    
    /**
     * Send a push notification to a specific device.
     * @param deviceToken FCM registration token (Android) or APNs device token (iOS)
     * @param title Notification title
     * @param body Notification body
     * @param data Additional data payload (optional)
     * @return true if sent successfully, false otherwise
     */
    suspend fun sendNotification(
        deviceToken: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean
    
    /**
     * Send a push notification to multiple devices (broadcast).
     * @param deviceTokens List of device tokens
     * @param title Notification title
     * @param body Notification body
     * @param data Additional data payload (optional)
     * @return number of successfully sent notifications
     */
    suspend fun sendBroadcast(
        deviceTokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Int
}
