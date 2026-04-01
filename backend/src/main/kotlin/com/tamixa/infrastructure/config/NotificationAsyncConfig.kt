package com.tamixa.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

/**
 * Bounded executor for outbound transactional email (passwordless / magic link).
 * Phase 2: decouples HTTP request thread from SendGrid latency; future extracted notifications service
 * can consume the same events from a queue.
 */
@Configuration
class NotificationAsyncConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean(name = ["notificationExecutor"])
    fun notificationExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 4
            queueCapacity = 500
            setThreadNamePrefix("notification-email-")
            setRejectedExecutionHandler { _, e ->
                log.error(
                    "Notification executor rejected task (queue full). active={} poolSize={}",
                    e.activeCount,
                    e.poolSize
                )
                throw RejectedExecutionException("Notification email executor overloaded")
            }
            initialize()
        }
        log.info("Notification email executor: core=2 max=4 queue=500 (async SendGrid)")
        return executor
    }
}
