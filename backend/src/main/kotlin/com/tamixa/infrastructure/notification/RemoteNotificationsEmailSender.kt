package com.tamixa.infrastructure.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.EmailSenderPort
import com.tamixa.infrastructure.config.AppProperties
import com.tamixa.infrastructure.logging.PiiMask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Delegates magic-link email to the extracted **tamixa-notifications** service (internal HTTP).
 * Enable with `app.notifications-email.remote-enabled=true` and set base-url + internal-api-key.
 */
@Component
@ConditionalOnProperty(name = ["app.notifications-email.remote-enabled"], havingValue = "true")
class RemoteNotificationsEmailSender(
    @Qualifier("notificationRestTemplate") private val rest: RestTemplate,
    private val appProperties: AppProperties
) : EmailSenderPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = ObjectMapper()

    override fun sendMagicLinkOrCode(toEmail: String, magicLink: String, shortCode: String): Boolean {
        val cfg = appProperties.notificationsEmail
        val base = cfg.baseUrl.trim().removeSuffix("/")
        if (base.isEmpty() || cfg.internalApiKey.isBlank()) {
            log.error("Remote notifications email enabled but base-url or internal-api-key is blank")
            return false
        }
        return try {
            val url = URI.create("$base/internal/v1/email/magic-link")
            val body = mapOf(
                "toEmail" to toEmail,
                "magicLink" to magicLink,
                "shortCode" to shortCode
            )
            val json = mapper.writeValueAsString(body)
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Internal-Api-Key", cfg.internalApiKey)
            }
            val response = rest.postForEntity(url, HttpEntity(json, headers), String::class.java)
            val ok = response.statusCode.is2xxSuccessful
            if (ok) {
                log.info("Magic link email delegated to notifications service for {}", PiiMask.maskEmail(toEmail))
            } else {
                log.warn(
                    "Notifications service returned {} for toEmail={}",
                    response.statusCode,
                    PiiMask.maskEmail(toEmail)
                )
            }
            ok
        } catch (e: Exception) {
            log.warn(
                "Failed to reach notifications service for toEmail={}: {}",
                PiiMask.maskEmail(toEmail),
                e.message
            )
            false
        }
    }
}
