package com.araro.infrastructure.config

import com.araro.application.port.SmsSenderPort
import com.araro.infrastructure.notification.LoggingSmsSender
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Ensures SmsSenderPort is always available. When Twilio is not configured,
 * provides LoggingSmsSender that logs OTP to stdout (dev use).
 */
@Configuration
class SmsSenderConfig {

    @Bean
    @ConditionalOnMissingBean(SmsSenderPort::class)
    fun loggingSmsSender(): SmsSenderPort = LoggingSmsSender()
}
