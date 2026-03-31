package com.tamixa.infrastructure.notification

import com.tamixa.application.port.PushNotificationPort
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Firebase Cloud Messaging (FCM) adapter for Android push notifications.
 * Enabled when FCM_SERVER_KEY is set.
 * 
 * Setup:
 * 1. Create Firebase project: https://console.firebase.google.com
 * 2. Add Android app with package name com.tamixa.android
 * 3. Download google-services.json to mobile/androidApp/
 * 4. Get Server Key from Project Settings > Cloud Messaging
 * 5. Set FCM_SERVER_KEY env var
 */
@Component
@ConditionalOnProperty(name = ["app.push.fcm-server-key"])
class FCMPushNotificationAdapter(
    @Qualifier("notificationRestTemplate") private val rest: RestTemplate,
    @Value("\${app.push.fcm-server-key}") private val serverKey: String
) : PushNotificationPort {
    
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = ObjectMapper()
    private val fcmUrl = "https://fcm.googleapis.com/fcm/send"
    
    override suspend fun sendNotification(
        deviceToken: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Boolean {
        return try {
            val payload = mapOf(
                "to" to deviceToken,
                "notification" to mapOf(
                    "title" to title,
                    "body" to body,
                    "sound" to "default"
                ),
                "data" to data,
                "priority" to "high"
            )
            
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "key=$serverKey")
            }
            
            val request = HttpEntity(mapper.writeValueAsString(payload), headers)
            val response = rest.postForEntity(fcmUrl, request, String::class.java)
            
            if (response.statusCode.is2xxSuccessful) {
                log.info("FCM notification sent successfully to token={}", maskToken(deviceToken))
                true
            } else {
                log.warn("FCM notification failed: status={} body={}", response.statusCode, response.body)
                false
            }
        } catch (e: Exception) {
            log.error("FCM notification error for token={}: {}", maskToken(deviceToken), e.message, e)
            false
        }
    }
    
    override suspend fun sendBroadcast(
        deviceTokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>
    ): Int {
        var successCount = 0
        deviceTokens.forEach { token ->
            if (sendNotification(token, title, body, data)) {
                successCount++
            }
        }
        log.info("FCM broadcast sent: {}/{} successful", successCount, deviceTokens.size)
        return successCount
    }
    
    private fun maskToken(token: String): String {
        return if (token.length > 10) "${token.take(6)}...${token.takeLast(4)}" else "***"
    }
}
