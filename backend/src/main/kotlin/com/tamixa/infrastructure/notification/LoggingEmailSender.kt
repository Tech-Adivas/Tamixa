package com.tamixa.infrastructure.notification

import com.tamixa.application.port.EmailSenderPort
import com.tamixa.infrastructure.logging.PiiMask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Logs magic link send attempt to stdout. Default when SendGrid is not configured.
 * When SENDGRID_API_KEY is set, SendGridEmailSender (@Primary) is used instead.
 * Security: never log magic link or code; mask email.
 */
@Component
class LoggingEmailSender : EmailSenderPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendMagicLinkOrCode(toEmail: String, magicLink: String, shortCode: String): Boolean {
        log.warn("Magic link (dev - not sent): to={}", PiiMask.maskEmail(toEmail))
        return true
    }
}
