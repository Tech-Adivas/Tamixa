package com.tamixa.infrastructure.guardrails

import com.fasterxml.jackson.annotation.JsonProperty
import com.tamixa.application.guardrail.ExternalGuardrailUnavailableException
import com.tamixa.application.port.StructuredStoryRemoteGuardrailPort
import com.tamixa.application.story.ContentModerationException
import com.tamixa.application.story.ModerationContext
import com.tamixa.application.story.StructuredStoryPayload
import com.tamixa.infrastructure.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Component
@ConditionalOnProperty(name = ["app.guardrails-service.enabled"], havingValue = "true")
class HttpStructuredStoryGuardrailAdapter(
    builder: RestTemplateBuilder,
    private val appProperties: AppProperties
) : StructuredStoryRemoteGuardrailPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate

    init {
        val g = appProperties.guardrailsService
        restTemplate = builder
            .setConnectTimeout(Duration.ofMillis(g.connectTimeoutMs))
            .setReadTimeout(Duration.ofMillis(g.readTimeoutMs))
            .build()
    }

    override fun validateStructuredStory(
        payload: StructuredStoryPayload,
        context: ModerationContext,
        maxStoryWordsAllowed: Int
    ) {
        val g = appProperties.guardrailsService
        val base = g.baseUrl.trim().trimEnd('/')
        if (base.isBlank()) {
            log.warn("app.guardrails-service.enabled=true but base-url is blank")
            if (!g.failOpenOnError) {
                throw ExternalGuardrailUnavailableException("Guardrails service base URL is not configured")
            }
            return
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            if (g.apiKey.isNotBlank()) {
                set("X-Guardrails-Api-Key", g.apiKey)
            }
        }
        val body = GuardrailsValidateRequest(
            consumerId = "tamixa",
            requestId = context.promptId,
            payload = GuardrailsStoryPayloadJson(
                title = payload.title,
                moral = payload.moral,
                storyText = payload.storyText,
                estimatedDurationSeconds = payload.estimatedDurationSeconds
            ),
            context = GuardrailsContextJson(
                language = context.language,
                age = context.age,
                maxStoryWords = maxStoryWordsAllowed
            )
        )
        val entity = HttpEntity(body, headers)
        val url = "$base/v1/validate/structured-story"
        try {
            val response = restTemplate.postForEntity(url, entity, GuardrailsValidateResponseJson::class.java)
            val resBody = response.body
            if (resBody == null) {
                handleTransportFailure(IllegalStateException("Empty response from guardrails service"), g.failOpenOnError)
                return
            }
            if (!resBody.valid) {
                val msg = resBody.errors.joinToString("; ") { it.message }
                throw ContentModerationException(msg.ifBlank { "External guardrail rejected structured story" })
            }
        } catch (e: ContentModerationException) {
            throw e
        } catch (e: ExternalGuardrailUnavailableException) {
            throw e
        } catch (e: RestClientException) {
            log.warn("Guardrails HTTP call failed url={}: {}", url, e.message)
            handleTransportFailure(e, g.failOpenOnError)
        } catch (e: Exception) {
            log.warn("Guardrails call failed url={}: {}", url, e.message)
            handleTransportFailure(e, g.failOpenOnError)
        }
    }

    private fun handleTransportFailure(cause: Throwable, failOpen: Boolean) {
        if (failOpen) {
            log.warn("Guardrails fail-open: skipping remote validation")
            return
        }
        throw ExternalGuardrailUnavailableException("Guardrails service unavailable", cause)
    }
}

private data class GuardrailsValidateRequest(
    @JsonProperty("consumer_id") val consumerId: String,
    @JsonProperty("request_id") val requestId: String,
    val payload: GuardrailsStoryPayloadJson,
    val context: GuardrailsContextJson
)

private data class GuardrailsStoryPayloadJson(
    val title: String,
    val moral: String,
    @JsonProperty("story_text") val storyText: String,
    @JsonProperty("estimated_duration_seconds") val estimatedDurationSeconds: Int
)

private data class GuardrailsContextJson(
    val language: String,
    val age: Int,
    @JsonProperty("max_story_words") val maxStoryWords: Int
)

private data class GuardrailsValidateResponseJson(
    val valid: Boolean,
    val errors: List<GuardrailsErrorJson> = emptyList()
)

private data class GuardrailsErrorJson(
    val code: String,
    val message: String
)
