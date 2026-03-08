package com.araro

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableRetry
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
class AraroApplication

fun main(args: Array<String>) {
    runApplication<AraroApplication>(*args)
}
