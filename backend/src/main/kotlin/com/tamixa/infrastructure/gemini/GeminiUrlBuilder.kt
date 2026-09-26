package com.tamixa.infrastructure.gemini

/**
 * Builds REST URLs for Gemini [generateContent](https://ai.google.dev/api/rest/v1beta/models.generateContent).
 *
 * Google exposes the same capability on two common surfaces:
 * - **Google AI (AI Studio / API key):** `generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
 * - **Vertex “publisher” API key route:** `aiplatform.googleapis.com/v1/publishers/google/models/{model}:generateContent`
 *
 * Opening `https://generativelanguage.googleapis.com/` in a browser returns 404 — there is no index page; only versioned paths exist.
 */
object GeminiUrlBuilder {

    enum class ApiUrlStyle {
        GOOGLE_AI,
        VERTEX_PUBLISHERS,
        ;

        companion object {
            fun parse(raw: String): ApiUrlStyle {
                val n = raw.trim().lowercase().replace('-', '_')
                return when (n) {
                    "vertex_publishers", "vertex", "aiplatform" -> VERTEX_PUBLISHERS
                    else -> GOOGLE_AI
                }
            }
        }
    }

    fun generateContentUrl(baseUrl: String, model: String, apiKeyQueryEncoded: String, style: String): String {
        val base = baseUrl.trimEnd('/')
        val m = model.trim().removePrefix("models/")
        return when (ApiUrlStyle.parse(style)) {
            ApiUrlStyle.VERTEX_PUBLISHERS ->
                "$base/v1/publishers/google/models/$m:generateContent?key=$apiKeyQueryEncoded"
            ApiUrlStyle.GOOGLE_AI ->
                "$base/v1beta/models/$m:generateContent?key=$apiKeyQueryEncoded"
        }
    }

    /** Dev connectivity check (list models / catalog). */
    fun modelsListUrl(baseUrl: String, apiKeyQueryEncoded: String, style: String): String {
        val base = baseUrl.trimEnd('/')
        return when (ApiUrlStyle.parse(style)) {
            ApiUrlStyle.VERTEX_PUBLISHERS ->
                "$base/v1/publishers/google/models?key=$apiKeyQueryEncoded"
            ApiUrlStyle.GOOGLE_AI ->
                "$base/v1beta/models?key=$apiKeyQueryEncoded"
        }
    }
}
