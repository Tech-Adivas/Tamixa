package com.araro.infrastructure.notification

import com.araro.application.port.EmailSenderPort
import com.araro.infrastructure.logging.PiiMask
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
