package com.tamixa.infrastructure.notification

import com.tamixa.application.port.EmailSenderPort
import com.tamixa.infrastructure.logging.PiiMask
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Dispatches magic-link / passwordless email on [notificationExecutor] so request threads are not blocked
 * on SendGrid (Phase 2 strangler step toward a dedicated notifications service).
 */
@Component
class NotificationEmailDispatcher(
    private val remoteNotificationsEmailSender: ObjectProvider<RemoteNotificationsEmailSender>,
    private val sendGridEmailSender: ObjectProvider<SendGridEmailSender>,
    private val loggingEmailSender: LoggingEmailSender
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun coreSender(): EmailSenderPort =
        remoteNotificationsEmailSender.ifAvailable
            ?: sendGridEmailSender.ifAvailable
            ?: loggingEmailSender

    @Async("notificationExecutor")
    fun dispatchMagicLinkOrCode(toEmail: String, magicLink: String, shortCode: String) {
        try {
            val ok = coreSender().sendMagicLinkOrCode(toEmail, magicLink, shortCode)
            if (!ok) {
                log.warn("Email provider returned false for toEmail={}", PiiMask.maskEmail(toEmail))
            }
        } catch (e: Exception) {
            log.error("Async email failed for toEmail={}", PiiMask.maskEmail(toEmail), e)
        }
    }
}
