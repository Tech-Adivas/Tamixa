package com.tamixa.repository

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Records requests and answers them with [handler]; plugins as in createKtorClient (retry on 5xx, timeouts, JSON). */
class VoiceTestServer(
    private val handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
) {
    val requests = mutableListOf<HttpRequestData>()

    val client = HttpClient(MockEngine { request ->
        requests += request
        handler(request)
    }) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) { socketTimeoutMillis = 30_000 }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 3)
            constantDelay(millis = 1)
        }
    }

    fun requestsTo(method: String, pathSuffix: String) =
        requests.filter { it.method.value == method && it.url.encodedPath.endsWith(pathSuffix) }
}

fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

fun voiceJson(id: Long, referenceFileName: String?) =
    """{"id":$id,"parentId":8,"createdAt":"2026-10-12T09:40:23Z"${referenceFileName?.let { ",\"referenceFileName\":\"$it\"" } ?: ""}}"""
