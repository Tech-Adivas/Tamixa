package com.tamixa.infrastructure.notification

import com.tamixa.application.port.SmsSenderPort
import com.tamixa.infrastructure.logging.PiiMask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Sends SMS via Twilio API. Enable with app.sms.twilio-account-sid and app.sms.twilio-auth-token.
 */
@Component
@org.springframework.context.annotation.Primary
@ConditionalOnProperty(name = ["app.sms.twilio-account-sid"])
class TwilioSmsSender(
    @Value("\${app.sms.twilio-account-sid}") private val accountSid: String,
    @Value("\${app.sms.twilio-auth-token}") private val authToken: String,
    @Value("\${app.sms.twilio-from-number:}") private val fromNumber: String
) : SmsSenderPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val rest = RestTemplate()

    override fun sendOtp(toPhone: String, code: String): Boolean {
        if (fromNumber.isBlank()) {
            log.info("Twilio from-number not configured; OTP not sent (dev). To={}", PiiMask.maskPhone(toPhone))
            return true
        }
        return try {
            val body = LinkedMultiValueMap<String, String>().apply {
                add("To", toPhone)
                add("From", fromNumber)
                add("Body", "Your Tamixa login code is: $code. Valid for 5 minutes.")
            }
            val headers = HttpHeaders().apply {
                setBasicAuth(accountSid, authToken)
                contentType = MediaType.APPLICATION_FORM_URLENCODED
            }
            rest.postForEntity(
                URI.create("https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"),
                org.springframework.http.HttpEntity(body, headers),
                String::class.java
            )
            log.info("OTP SMS sent To={}", PiiMask.maskPhone(toPhone))
            true
        } catch (e: Exception) {
            log.warn("Failed to send OTP SMS To={}: {}", PiiMask.maskPhone(toPhone), e.message)
            false
        }
    }
}
