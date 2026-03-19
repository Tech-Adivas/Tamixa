package com.tamixa.application.port

/**
 * Sends transactional emails (magic links, passwordless codes).
 * Implementations: LoggingEmailSender (dev), SendGridEmailSender (prod when SENDGRID_API_KEY set).
 */
interface EmailSenderPort {
    /** Sends magic link / code email. Returns true if sent successfully. */
    fun sendMagicLinkOrCode(toEmail: String, magicLink: String, shortCode: String): Boolean
}
