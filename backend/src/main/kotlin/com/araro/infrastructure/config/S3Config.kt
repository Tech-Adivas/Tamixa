package com.araro.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.File

@Configuration
@ConditionalOnProperty(name = ["app.storage.type"], havingValue = "s3")
class S3Config {

    private val log = LoggerFactory.getLogger(javaClass)

    private fun resolveAwsCredentials(env: Environment): Pair<String?, String?> {
        var accessKey = env.getProperty("AWS_ACCESS_KEY_ID")
        var secretKey = env.getProperty("AWS_SECRET_ACCESS_KEY")
        if (accessKey.isNullOrBlank() || secretKey.isNullOrBlank()) {
            // Fallback: load from .env (works when run from IDE or Gradle without env pass-through)
            val cwd = File(System.getProperty("user.dir"))
            val candidates = listOf(cwd.resolve(".env"), cwd.resolve("../.env"), cwd.resolve("../../.env"))
            log.info("S3Config: AWS credentials not in env. Checking .env at cwd={} paths={}", cwd.absolutePath, candidates.map { it.absolutePath })
            val envFile = candidates.firstOrNull { it.exists() }
            if (envFile != null) {
                log.info("S3Config: Loading AWS credentials from .env at {}", envFile.absolutePath)
                envFile.readLines().forEach { line ->
                    val t = line.trim()
                    if (t.isNotEmpty() && !t.startsWith("#") && t.contains("=")) {
                        val eq = t.indexOf('=')
                        val k = t.substring(0, eq).trim()
                        var v = t.substring(eq + 1).trim()
                        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) v = v.drop(1).dropLast(1)
                        when (k) {
                            "AWS_ACCESS_KEY_ID" -> accessKey = v
                            "AWS_SECRET_ACCESS_KEY" -> secretKey = v
                        }
                    }
                }
            } else {
                log.warn("No AWS credentials in env. Tried .env at cwd={} and parent dirs. Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY or add .env at project root.", cwd.absolutePath)
            }
        }
        return accessKey to secretKey
    }

    @Bean
    fun s3Client(appProperties: AppProperties, env: Environment): S3Client {
        log.info("S3Config: initializing S3Client (storage.type={})", appProperties.storage.type)
        val region = Region.of(appProperties.storage.s3Region.ifBlank { "us-east-1" })
        val (accessKey, secretKey) = resolveAwsCredentials(env)
        val builder = S3Client.builder().region(region)
        if (!accessKey.isNullOrBlank() && !secretKey.isNullOrBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
            log.info("S3 client configured: bucket={} region={} (explicit credentials)", appProperties.storage.s3Bucket, region)
        } else {
            log.info("S3 client configured: bucket={} region={} (default credential chain)", appProperties.storage.s3Bucket, region)
        }
        return builder.build()
    }

    @Bean
    fun s3Presigner(appProperties: AppProperties, env: Environment): S3Presigner {
        val region = Region.of(appProperties.storage.s3Region.ifBlank { "us-east-1" })
        val (accessKey, secretKey) = resolveAwsCredentials(env)
        val builder = S3Presigner.builder().region(region)
        if (!accessKey.isNullOrBlank() && !secretKey.isNullOrBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        }
        return builder.build()
    }
}
