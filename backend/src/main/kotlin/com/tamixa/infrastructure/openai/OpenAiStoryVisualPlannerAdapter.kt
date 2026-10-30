package com.tamixa.infrastructure.openai

import com.fasterxml.jackson.databind.ObjectMapper
import com.tamixa.application.port.visuals.PlannerMessage
import com.tamixa.application.port.visuals.StoryVisualPlannerLlmPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/** Story slideshow planner on OpenAI chat completions in JSON mode (app.story-visuals.planner-model). */
@Component
class OpenAiStoryVisualPlannerAdapter(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    @Value("\${app.openai.api-key:}") private val apiKey: String,
    @Value("\${app.openai.base-url:https://api.openai.com}") private val baseUrl: String,
    @Value("\${app.story-visuals.planner-model:gpt-4o}") private val model: String,
) : StoryVisualPlannerLlmPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun completeJson(messages: List<PlannerMessage>): String? {
        if (apiKey.isBlank()) {
            log.warn("Story visual planner skipped: OPENAI_API_KEY is blank")
            return null
        }
        val body = mapOf(
            "model" to model.trim().ifBlank { "gpt-4o" },
            "temperature" to 0.3,
            "max_tokens" to 8000,
            "response_format" to mapOf("type" to "json_object"),
            "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) },
        )
        val headers = HttpHeaders().apply {
            setBearerAuth(apiKey)
            contentType = MediaType.APPLICATION_JSON
        }
        return try {
            val json = restTemplate.postForObject("${baseUrl.trimEnd('/')}/v1/chat/completions", HttpEntity(body, headers), String::class.java)
                ?: return null
            val root = objectMapper.readTree(json)
            val usage = root.path("usage")
            log.info(
                "Story visual planner reply model={} promptTokens={} completionTokens={}",
                model, usage.path("prompt_tokens").asInt(), usage.path("completion_tokens").asInt()
            )
            root.path("choices").path(0).path("message").path("content").asText(null)
        } catch (e: Exception) {
            log.warn("Story visual planner call failed: {}", e.message)
            null
        }
    }
}
