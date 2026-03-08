package com.araro.infrastructure.config

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class RestTemplateConfig {

    @Bean
    fun restTemplate(
        builder: RestTemplateBuilder,
        appProperties: AppProperties
    ): RestTemplate {
        val openai = appProperties.openai
        return builder
            .setConnectTimeout(Duration.ofMillis(openai.connectTimeoutMs))
            .setReadTimeout(Duration.ofMillis(openai.readTimeoutMs))
            .build()
    }
}
