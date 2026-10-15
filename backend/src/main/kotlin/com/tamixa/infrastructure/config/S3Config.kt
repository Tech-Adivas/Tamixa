package com.tamixa.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.File
import java.net.URI

@Configuration
@ConditionalOnExpression("@environment.getProperty('app.storage.type','s3').trim().toLowerCase() == 's3'")
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
                val onRailway = System.getenv().keys.any { it.startsWith("RAILWAY_") }
                if (onRailway) {
                    log.info(
                        "S3: no AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY in process env and no .env in the image (expected on Railway). " +
                            "Add AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, S3_BUCKET, and S3_REGION to this service's Variables, " +
                            "or use an IAM role only if your runtime provides the AWS SDK default credential chain."
                    )
                } else {
                    log.warn(
                        "No AWS credentials in env. Tried .env at cwd={} and parent dirs. " +
                            "Set AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY or add .env at project root.",
                        cwd.absolutePath,
                    )
                }
            }
        }
        return accessKey to secretKey
    }

    /** Custom S3-compatible endpoint (e.g. local MinIO), or null for real AWS S3. */
    private fun customEndpoint(appProperties: AppProperties): URI? =
        appProperties.storage.s3Endpoint.trim().takeIf { it.isNotEmpty() }?.let { URI.create(it) }

    /** Endpoint-only credentials, so AWS keys are never sent to a custom endpoint. */
    private fun endpointCredentials(appProperties: AppProperties): StaticCredentialsProvider? {
        val accessKey = appProperties.storage.s3EndpointAccessKey.trim()
        val secretKey = appProperties.storage.s3EndpointSecretKey.trim()
        if (accessKey.isEmpty() || secretKey.isEmpty()) return null
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))
    }

    @Bean
    fun s3Client(appProperties: AppProperties, env: Environment): S3Client {
        log.info("S3Config: initializing S3Client (storage.type={})", appProperties.storage.type)
        val region = Region.of(appProperties.storage.s3Region.ifBlank { "us-east-1" })
        val endpoint = customEndpoint(appProperties)
        if (endpoint != null) {
            // S3-compatible server (MinIO): path-style addressing, endpoint-only credentials.
            val builder = S3Client.builder()
                .region(region)
                .endpointOverride(endpoint)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            endpointCredentials(appProperties)?.let { builder.credentialsProvider(it) }
                ?: log.warn("S3 custom endpoint {} set but S3_ENDPOINT_ACCESS_KEY/S3_ENDPOINT_SECRET_KEY are missing", endpoint)
            log.info("S3 client configured: bucket={} endpoint={} (custom S3-compatible endpoint)", appProperties.storage.s3Bucket, endpoint)
            return builder.build()
        }
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
        val endpoint = customEndpoint(appProperties)
        if (endpoint != null) {
            val builder = S3Presigner.builder()
                .region(region)
                .endpointOverride(endpoint)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            endpointCredentials(appProperties)?.let { builder.credentialsProvider(it) }
            return builder.build()
        }
        val (accessKey, secretKey) = resolveAwsCredentials(env)
        val builder = S3Presigner.builder().region(region)
        if (!accessKey.isNullOrBlank() && !secretKey.isNullOrBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        }
        return builder.build()
    }
}
