package com.tamixa.infrastructure.config

import com.tamixa.infrastructure.avatar.DidAvatarVideoCondition
import com.tamixa.infrastructure.avatar.GooeyLipSyncCondition
import com.tamixa.infrastructure.avatar.ReplicateSadTalkerCondition
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Conditional
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.time.Duration

@Configuration
class RestTemplateConfig {

    private val log = LoggerFactory.getLogger(RestTemplateConfig::class.java)

    /**
     * Outbound HTTP for SendGrid, Twilio, FCM, APNs — bounded timeouts so slow providers do not pin servlet threads indefinitely.
     */
    @Bean("notificationRestTemplate")
    fun notificationRestTemplate(builder: RestTemplateBuilder): RestTemplate =
        builder
            .setConnectTimeout(Duration.ofSeconds(15))
            .setReadTimeout(Duration.ofSeconds(60))
            .build()

    @Bean
    fun restTemplate(
        builder: RestTemplateBuilder,
        appProperties: AppProperties
    ): RestTemplate {
        val geminiHttp =
            appProperties.llm.provider.trim().equals("gemini", ignoreCase = true) ||
                appProperties.imageGeneration.provider.trim().equals("gemini", ignoreCase = true)
        val (connectMs, readMs) =
            if (geminiHttp) {
                val g = appProperties.llm.gemini
                g.connectTimeoutMs to g.readTimeoutMs
            } else {
                val o = appProperties.openai
                o.connectTimeoutMs to o.readTimeoutMs
            }
        return builder
            .setConnectTimeout(Duration.ofMillis(connectMs))
            .setReadTimeout(Duration.ofMillis(readMs))
            .additionalInterceptors(RateLimitLoggingInterceptor())
            .build()
    }

    /** RestTemplate for Gooey lip-sync API: uses app.avatar-video timeouts. */
    @Bean("gooeyRestTemplate")
    @Conditional(GooeyLipSyncCondition::class)
    fun gooeyRestTemplate(
        builder: RestTemplateBuilder,
        appProperties: AppProperties
    ): RestTemplate {
        val av = appProperties.avatarVideo
        return builder
            .setConnectTimeout(Duration.ofMillis(av.connectTimeoutMs))
            .setReadTimeout(Duration.ofMillis(av.readTimeoutMs))
            .build()
    }

    /** RestTemplate for HeyGen (avatar + optional voice TTS): uses app.avatar-video timeouts. */
    @Bean("heygenRestTemplate")
    @ConditionalOnProperty(name = ["app.avatar-video.provider"], havingValue = "heygen")
    fun heygenRestTemplate(
        builder: RestTemplateBuilder,
        appProperties: AppProperties
    ): RestTemplate {
        val av = appProperties.avatarVideo
        return builder
            .setConnectTimeout(Duration.ofMillis(av.connectTimeoutMs))
            .setReadTimeout(Duration.ofMillis(av.readTimeoutMs))
            .build()
    }

    /** RestTemplate for D-ID Talks API: uses app.avatar-video timeouts. */
    @Bean("didRestTemplate")
    @Conditional(DidAvatarVideoCondition::class)
    fun didRestTemplate(
        builder: RestTemplateBuilder,
        appProperties: AppProperties
    ): RestTemplate {
        val av = appProperties.avatarVideo
        return builder
            .setConnectTimeout(Duration.ofMillis(av.connectTimeoutMs))
            .setReadTimeout(Duration.ofMillis(av.readTimeoutMs))
            .build()
    }

    /** RestTemplate for Replicate (SadTalker) API: uses app.avatar-video timeouts. */
    @Bean("replicateRestTemplate")
    @Conditional(ReplicateSadTalkerCondition::class)
    fun replicateRestTemplate(
        builder: RestTemplateBuilder,
        appProperties: AppProperties
    ): RestTemplate {
        val av = appProperties.avatarVideo
        return builder
            .setConnectTimeout(Duration.ofMillis(av.connectTimeoutMs))
            .setReadTimeout(Duration.ofMillis(av.readTimeoutMs))
            .build()
    }

    /**
     * Google Cloud Text-to-Speech HTTP client (narration + voice-cloning key API share the same endpoint style).
     * Must exist whenever [GoogleCloudTtsClientAdapter] OR [GoogleCloudVoiceCloningAdapter] is active.
     * Default `app.voice-cloning.provider` is `google` while `app.narration.tts-provider` may stay `tamixa` — without this,
     * the context fails with "No qualifying bean ... googleTtsRestTemplate".
     */
    @Bean("googleTtsRestTemplate")
    @ConditionalOnExpression(
        "'\${app.narration.tts-provider:tamixa}'.equalsIgnoreCase('google') || " +
            "'\${app.voice-cloning.provider:google}'.equalsIgnoreCase('google')",
    )
    fun googleTtsRestTemplate(
        builder: RestTemplateBuilder,
        appProperties: AppProperties
    ): RestTemplate {
        val readSec = appProperties.narration.googleTtsReadTimeoutSeconds.coerceIn(60, 300)
        log.info("Google TTS RestTemplate: connect=15s, read={}s (per chunk; full story = multiple chunks)", readSec)
        return builder
            .setConnectTimeout(Duration.ofSeconds(15))
            .setReadTimeout(Duration.ofSeconds(readSec.toLong()))
            .build()
    }

    /** RestTemplate for XTTS calls: long read timeout (synthesis can take 1–2 min, especially on first request). */
    @Bean("xttsRestTemplate")
    @ConditionalOnProperty(name = ["app.voice-cloning.xtts-base-url"])
    fun xttsRestTemplate(
        builder: RestTemplateBuilder,
        appProperties: AppProperties
    ): RestTemplate {
        val sec = appProperties.voiceCloning.xttsTimeoutSeconds.coerceIn(60, 600)
        log.info("XTTS RestTemplate: connect=15s, read={}s", sec)
        return builder
            .setConnectTimeout(Duration.ofSeconds(15))
            .setReadTimeout(Duration.ofSeconds(sec.toLong()))
            .build()
    }

    /**
     * Logs 429 TOO_MANY_REQUESTS from outbound calls with URI and Retry-After so operators
     * can see which upstream (OpenAI, TTS, etc.) is rate-limiting. Response is not consumed;
     * RestTemplate will still throw and callers can retry (e.g. @Retryable).
     */
    private inner class RateLimitLoggingInterceptor : ClientHttpRequestInterceptor {
        @Throws(IOException::class)
        override fun intercept(
            request: HttpRequest,
            body: ByteArray,
            execution: ClientHttpRequestExecution
        ): ClientHttpResponse {
            val response = execution.execute(request, body)
            if (response.statusCode.value() == 429) {
                val uri = request.uri.toString()
                val retryAfter = response.headers.getFirst("Retry-After")
                log.warn(
                    "Upstream rate limit (429 TOO_MANY_REQUESTS) from {}; Retry-After={}. " +
                        "Consider lowering app.narration.max-concurrent-tts / max-concurrent-rewrite or retrying later.",
                    uri,
                    retryAfter ?: "none"
                )
            }
            return response
        }
    }
}
