package com.tamixa.infrastructure.notification

import com.tamixa.application.port.EmailSenderPort
import com.tamixa.infrastructure.logging.PiiMask
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Sends email via SendGrid API. Enable with app.email.sendgrid-api-key (non-empty).
 * When key is empty/unset, LoggingEmailSender is used instead (logs code to stdout).
 */
@Component
@org.springframework.context.annotation.Primary
@org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("!'${'$'}{app.email.sendgrid-api-key:}'.isEmpty()")
class SendGridEmailSender(
    @Value("\${app.email.sendgrid-api-key}") private val apiKey: String,
    @Value("\${app.email.from-email:noreply@tamixa.in}") private val fromEmail: String,
    @Value("\${app.email.from-name:Tamixa}") private val fromName: String
) : EmailSenderPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val rest = RestTemplate()
    private val mapper = ObjectMapper()

    override fun sendMagicLinkOrCode(toEmail: String, magicLink: String, shortCode: String): Boolean {
        return try {
            val payload = mapOf(
                "personalizations" to listOf(mapOf("to" to listOf(mapOf("email" to toEmail)))),
                "from" to mapOf("email" to fromEmail, "name" to fromName),
                "subject" to "Your Tamixa login code",
                "content" to listOf(
                    mapOf(
                        "type" to "text/plain",
                        "value" to "Your login code is: $shortCode\n\nOr click here to sign in: $magicLink\n\nThis code expires in 15 minutes."
                    )
                )
            )
            val body = mapper.writeValueAsString(payload)
            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $apiKey")
                contentType = MediaType.APPLICATION_JSON
            }
            rest.postForEntity(
                URI.create("https://api.sendgrid.com/v3/mail/send"),
                org.springframework.http.HttpEntity(body, headers),
                String::class.java
            )
            log.info("Magic link email sent to {}", PiiMask.maskEmail(toEmail))
            true
        } catch (e: Exception) {
            log.warn("Failed to send magic link email to {}: {}", PiiMask.maskEmail(toEmail), e.message)
            false
        }
    }
}
