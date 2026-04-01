package com.tamixa.infrastructure.notification

import com.tamixa.application.port.EmailSenderPort
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * [EmailSenderPort] facade for application code. Queues delivery on [notificationExecutor];
 * returns immediately with true when the task is accepted (anti-enumeration contract for passwordless).
 */
@Component
@Primary
class AsyncEmailSenderFacade(
    private val dispatcher: NotificationEmailDispatcher
) : EmailSenderPort {

    override fun sendMagicLinkOrCode(toEmail: String, magicLink: String, shortCode: String): Boolean {
        dispatcher.dispatchMagicLinkOrCode(toEmail, magicLink, shortCode)
        return true
    }
}
