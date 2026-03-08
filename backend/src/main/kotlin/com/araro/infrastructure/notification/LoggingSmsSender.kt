package com.araro.infrastructure.notification

import com.araro.application.port.SmsSenderPort
import com.araro.infrastructure.logging.PiiMask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Logs OTP send attempt to stdout. Use when no real SMS provider is configured.
 * Bean created via SmsSenderConfig when Twilio is not configured.
 * Security: never log OTP code; mask phone.
 */
class LoggingSmsSender : SmsSenderPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendOtp(toPhone: String, code: String): Boolean {
        log.info("SMS (dev - not sent): to={}", PiiMask.maskPhone(toPhone))
        return true
    }
}
