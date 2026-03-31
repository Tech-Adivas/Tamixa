package com.tamixa.infrastructure.notification

import com.tamixa.application.port.PushNotificationPort
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.time.Instant

/**
 * Apple Push Notification service (APNs) adapter for iOS push notifications.
 * Enabled when APNS_KEY_ID, APNS_TEAM_ID, and APNS_KEY_PATH are set.
 * 
 * Setup:
 * 1. Create APNs Auth Key in Apple Developer Portal
 * 2. Download .p8 key file
 * 3. Set env vars:
 *    - APNS_ENABLED: true (required; otherwise the bean stays off even if keys are present in config)
 *    - APNS_KEY_ID: 10-character key ID from portal
 *    - APNS_TEAM_ID: 10-character team ID
 *    - APNS_KEY_PATH: path to .p8 file
 *    - APNS_BUNDLE_ID: app bundle ID (com.tamixa.ios)
 *    - APNS_PRODUCTION: true for prod, false for sandbox
 */
@Component
@ConditionalOnProperty(name = ["app.push.apns-enabled"], havingValue = "true")
class APNsPushNotificationAdapter(
    @Qualifier("notificationRestTemplate") private val rest: RestTemplate,
    @Value("\${app.push.apns-key-id}") private val keyId: String,
    @Value("\${app.push.apns-team-id}") private val teamId: String,
    @Value("\${app.push.apns-key-path}") private val keyPath: String,
    @Value("\${app.push.apns-bundle-id:com.tamixa.ios}") private val bundleId: String,
    @Value("\${app.push.apns-production:false}") private val isProduction: Boolean
) : PushNotificationPort {
    
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = ObjectMapper()
    private val privateKey: PrivateKey = loadPrivateKey()
    private val apnsUrl = if (isProduction) 
        "https://api.push.apple.com" 
    else 
        "https://api.sandbox.push.apple.com"
    
    private fun loadPrivateKey(): PrivateKey {
        val keyContent = Files.readString(Paths.get(keyPath))
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        
        val keyBytes = Base64.getDecoder().decode(keyContent)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("EC").generatePrivate(spec)
    }
    
    private fun generateJWT(): String {
        val now = Instant.now()
        return Jwts.builder()
            .setIssuer(teamId)
            .setIssuedAt(java.util.Date.from(now))
            .setHeaderParam("kid", keyId)
            .signWith(privateKey, SignatureAlgorithm.ES256)
            .compact()
    }
    
    override suspend fun sendNotification(
        deviceToken: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Boolean {
        return try {
            val payload = mapOf(
                "aps" to mapOf(
                    "alert" to mapOf(
                        "title" to title,
                        "body" to body
                    ),
                    "sound" to "default",
                    "badge" to 1
                ),
                "data" to data
            )
            
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("authorization", "bearer ${generateJWT()}")
                set("apns-topic", bundleId)
                set("apns-push-type", "alert")
                set("apns-priority", "10")
            }
            
            val url = "$apnsUrl/3/device/$deviceToken"
            val request = HttpEntity(mapper.writeValueAsString(payload), headers)
            val response = rest.exchange(url, HttpMethod.POST, request, String::class.java)
            
            if (response.statusCode.is2xxSuccessful) {
                log.info("APNs notification sent successfully to token={}", maskToken(deviceToken))
                true
            } else {
                log.warn("APNs notification failed: status={} body={}", response.statusCode, response.body)
                false
            }
        } catch (e: Exception) {
            log.error("APNs notification error for token={}: {}", maskToken(deviceToken), e.message, e)
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
        log.info("APNs broadcast sent: {}/{} successful", successCount, deviceTokens.size)
        return successCount
    }
    
    private fun maskToken(token: String): String {
        return if (token.length > 10) "${token.take(6)}...${token.takeLast(4)}" else "***"
    }
}
