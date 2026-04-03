package com.tamixa.ui

import com.tamixa.network.ContentModerationFailureException
import com.tamixa.network.StoryGenerateBadRequestException
import com.tamixa.network.StoryValidationServiceUnavailableException
import com.tamixa.ui.strings.Strings
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException

/**
 * Returns a user-facing error message. Does not expose technical causes.
 * - 401: session expired (triggers navigation flow)
 * - Timeout: distinct message so users know to retry
 * - Network/connection: offline message
 * - Everything else: generic "something went wrong"
 */
fun errorMessageForUser(throwable: Throwable): String {
    val cause = throwable.cause ?: throwable
    return when {
        cause is StoryGenerateBadRequestException -> {
            val ex = cause as StoryGenerateBadRequestException
            when (ex.apiCode) {
                "UNKNOWN_GENERATION_TOPIC" -> Strings.storyGenerateUnknownTopic()
                "THEME_OR_TOPIC_REQUIRED" -> Strings.storyGenerateThemeOrTopicRequired()
                "GENERATION_LANGUAGE_NOT_SUPPORTED" -> Strings.storyGenerateTamilOnly()
                else -> ex.message?.takeIf { it.isNotBlank() } ?: Strings.somethingWentWrong()
            }
        }
        cause is ContentModerationFailureException ->
            cause.message?.takeIf { it.isNotBlank() } ?: Strings.storyContentNotAllowed()
        cause is StoryValidationServiceUnavailableException ->
            cause.message?.takeIf { it.isNotBlank() } ?: Strings.storyValidationTemporarilyUnavailable()
        cause is HttpRequestTimeoutException || isTimeoutError(cause) ->
            Strings.requestTimedOut()
        cause is ResponseException && cause.response.status.value == 401 ->
            Strings.sessionExpired()
        cause is ResponseException && cause.response.status.value >= 500 ->
            Strings.serverError()
        isNetworkError(cause) || isConnectionError(cause) ->
            Strings.noInternetConnection()
        else ->
            Strings.somethingWentWrong()
    }
}

/** True if this is a network/connection/server error (for logging or internal use only). */
fun isApiOrNetworkError(throwable: Throwable): Boolean {
    val cause = throwable.cause ?: throwable
    return cause is ResponseException || isNetworkError(cause) || isConnectionError(cause)
}

private fun isTimeoutError(t: Throwable): Boolean {
    val name = t::class.simpleName ?: ""
    return "Timeout" in name || "timeout" in (t.message ?: "")
}

private fun isNetworkError(t: Throwable): Boolean {
    val name = t::class.simpleName ?: ""
    return name == "UnknownHostException" || "UnknownHost" in name ||
        "Could not resolve host" in (t.message ?: "")
}

private fun isConnectionError(t: Throwable): Boolean {
    val name = t::class.simpleName ?: ""
    return name == "IOException" || "ConnectException" in name || "Socket" in name ||
        "Connection" in (t.message ?: "") || "connection" in (t.message ?: "")
}
